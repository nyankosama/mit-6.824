package viewservice

import "net"
import "net/rpc"
import "log"
import "time"
import "sync"
import "fmt"
import "strconv"
import "os"
import "sync/atomic"

type PingServer struct {
	addr       string
	timeOutNum int32
}

func (ps *PingServer) addTick() {
	atomic.AddInt32(&ps.timeOutNum, 1)
}

func NewPingServer(addr string, timeOutNum int32) *PingServer {
	ps := new(PingServer)
	ps.addr = addr
	ps.timeOutNum = timeOutNum
	return ps
}

//viewservice 的状态
const (
	VS_NO_PRI          = 0
	VS_PRIACK_WAIT     = 1
	VS_PRIACK_RECEIVED = 2
)

type ViewServer struct {
	mu       sync.Mutex
	l        net.Listener
	dead     int32 // for testing
	rpccount int32 // for testing
	me       string

	// Your declarations here.
	currentView    *View //write on copy
	nextView       *View //保存在VS_PRIACK_WAIT状态下进行的View的change
	primary        *PingServer
	backup         *PingServer
	status         int
	idleServersMap map[string]*PingServer
}

func mapGetRandomVal(m map[string]*PingServer) *PingServer {
	for _, v := range m {
		return v
	}
	return nil
}

func (vs *ViewServer) contains(addr string) bool {
	return vs.hasPrimary(addr) || vs.hasBackup(addr) || vs.hasIdle(addr)
}

func (vs *ViewServer) hasPrimary(addr string) bool {
	return vs.primary != nil && vs.primary.addr == addr
}

func (vs *ViewServer) hasBackup(addr string) bool {
	return vs.backup != nil && vs.backup.addr == addr
}

func (vs *ViewServer) hasIdle(addr string) bool {
	_, ok := vs.idleServersMap[addr]
	return ok
}

func (vs *ViewServer) handleTick(addr string) {
	if vs.primary != nil && vs.primary.addr == addr {
		vs.primary.timeOutNum = 0
	} else if vs.backup != nil && vs.backup.addr == addr {
		vs.backup.timeOutNum = 0
	} else {
		v, ok := vs.idleServersMap[addr]
		if ok {
			v.timeOutNum = 0
		}
	}
}

//
// server Ping RPC handler.
//可能的状态有
// * (0,0)0 初始态或者所有服务都失效
// * (1,0)0 只有primary 没有backup
// * (1,1)0 有primary和backup没有idleserver
// * (1,1)1 有primary和backup以及idleserver
//
func (vs *ViewServer) Ping(args *PingArgs, reply *PingReply) error {
	log.Println("==========================================================")
	log.Println("received:" + args.Me + ", vn=" + strconv.Itoa(int(args.Viewnum)))
	vs.mu.Lock()
	defer vs.mu.Unlock()
	//handle tick
	vs.handleTick(args.Me)
	//判断是否viewservice在初始状态
	vn := vs.currentView.Viewnum
	if vs.status == VS_NO_PRI {
		log.Println("viewservice VS_NO_PRI status")
		vs.currentView = NewView(vn+1, args.Me, "")
		vs.primary = NewPingServer(args.Me, 0)
		vs.status = VS_PRIACK_WAIT
	} else if vs.status == VS_PRIACK_WAIT {
		log.Println("viewservice VS_PRIACK_WAIT status")
		if vs.hasPrimary(args.Me) {
			vs.status = VS_PRIACK_RECEIVED
			if vs.nextView != nil {
				vs.currentView = vs.nextView
				vs.nextView = nil
			}
		} else if !vs.contains(args.Me) {
			//分为两种情况
			//(1,0)0
			//(1,1)*
			if vs.currentView.Backup == "" {
				//保存到nextView中，等待接受到priack
				vs.nextView = NewView(vn+1, vs.currentView.Primary, args.Me)
				vs.backup = NewPingServer(args.Me, 0)
			} else {
				ps := NewPingServer(args.Me, 0)
				vs.idleServersMap[args.Me] = ps
			}
		}
	} else {
		//VS_PRIACK_RECEIVED状态下分为几种情况，即
		//(1,0)0
		//(1,1)*
		//(d,0)*
		//(d,1)*
		//(1,d)*
		log.Println("viewservice VS_PRIACK_RECEIVED status")
		if !vs.contains(args.Me) {
			if vs.currentView.Backup == "" {
				log.Println("viewservice set backup")
				vs.currentView = NewView(vn+1, vs.currentView.Primary, args.Me)
				vs.backup = NewPingServer(args.Me, 0)
			} else {
				log.Println("viewservice append idleServers")
				//有back，直接放到idleServers里面
				ps := NewPingServer(args.Me, 0)
				vs.idleServersMap[args.Me] = ps
			}
		} else if args.Viewnum == 0 {
			//restarted server
			if vs.hasPrimary(args.Me) && vs.backup != nil {
				log.Println("primary fail:" + vs.primary.addr + ", currentView:" + vs.currentView.String())
				if len(vs.idleServersMap) != 0 {
					ids := mapGetRandomVal(vs.idleServersMap)
					vs.currentView = NewView(vn+1, vs.backup.addr, ids.addr)
					vs.primary = vs.backup
					vs.backup = ids
				} else {
					vs.currentView = NewView(vn+1, vs.backup.addr, "")
					vs.primary = vs.backup
					vs.backup = nil
				}
				vs.status = VS_PRIACK_WAIT
			} else if vs.hasBackup(args.Me) {
				log.Println("backup fail:" + vs.backup.addr + ", currentView:" + vs.currentView.String())
				if len(vs.idleServersMap) != 0 {
					ids := mapGetRandomVal(vs.idleServersMap)
					vs.currentView = NewView(vn+1, vs.primary.addr, ids.addr)
					vs.backup = ids
				} else {
					vs.currentView = NewView(vn+1, vs.primary.addr, "")
					vs.backup = nil
				}
			}
		}
	}
	log.Println("now currentView:" + vs.currentView.String())
	log.Println("now status:" + strconv.Itoa(vs.status))
	log.Println("==========================================================")
	reply.View = *vs.currentView
	return nil
}

//
// server Get() RPC handler.
//
func (vs *ViewServer) Get(args *GetArgs, reply *GetReply) error {
	reply.View = *vs.currentView
	return nil
}

//
// tick() is called once per PingInterval; it should notice
// if servers have died or recovered, and change the view
// accordingly.
//
func (vs *ViewServer) tick() {
	//增加记数
	vs.mu.Lock()
	defer vs.mu.Unlock()
	if vs.primary != nil {
		vs.primary.addTick()
	}
	if vs.backup != nil {
		vs.backup.addTick()
	}
	for _, server := range vs.idleServersMap {
		server.addTick()
	}
	//处理tick数达到上限的server
	vn := vs.currentView.Viewnum
	//只在status == VS_PRIACK_RECEIVED时才去改变相应的状态
	if vs.status != VS_PRIACK_RECEIVED {
		return
	}
	if vs.primary != nil && vs.primary.timeOutNum == DeadPings {
		log.Println("primary fail:" + vs.primary.addr + ", currentView:" + vs.currentView.String())
		if vs.currentView.Backup != "" {
			if len(vs.idleServersMap) != 0 {
				//(d,1)* -> (1,1)*-1
				ids := mapGetRandomVal(vs.idleServersMap)
				log.Println("(d,1)* -> (1,1)*-1" + " backup:" + vs.backup.addr + ", idle:" + ids.addr)
				delete(vs.idleServersMap, ids.addr)
				vs.currentView = NewView(vn+1, vs.currentView.Backup, ids.addr)
				vs.primary = vs.backup
				vs.backup = ids
				vs.status = VS_PRIACK_WAIT
			} else {
				//(d,1)0 -> (1,0)0
				log.Println("(d,1)0 -> (1,0)0" + " backup:" + vs.backup.addr)
				vs.currentView = NewView(vn+1, vs.currentView.Backup, "")
				vs.primary = vs.backup
				vs.backup = nil
				vs.status = VS_PRIACK_WAIT
			}
		} else {
			//(d,0) -> (0,0)
			vs.currentView = NewView(vn+1, "", "")
			vs.status = VS_NO_PRI
			vs.primary = nil
			log.Println("(d,0) -> (0,0)")
		}
		log.Println("tick currentView:" + vs.currentView.String())
	}
	if vs.backup != nil && vs.backup.timeOutNum == DeadPings {
		log.Println("backup fail:" + vs.backup.addr + ", currentView:" + vs.currentView.String())
		if len(vs.idleServersMap) != 0 {
			//(1,d)* -> (1,1)*-1
			ids := mapGetRandomVal(vs.idleServersMap)
			delete(vs.idleServersMap, ids.addr)
			vs.currentView = NewView(vn+1, vs.currentView.Primary, ids.addr)
			vs.backup = ids
			log.Println("(1,d)* -> (1,1)*-1" + " idle:" + ids.addr)
		} else {
			//(1,d)0 -> (1,0)0
			log.Println("(1,d)0 -> (1,0)0")
			vs.currentView = NewView(vn+1, vs.currentView.Primary, "")
			vs.backup = nil
		}
		log.Println("tick currentView:" + vs.currentView.String())
	}
	for key, val := range vs.idleServersMap {
		//FIXME 直接使用iter进行remove
		//(*,*)*d -> (*,*)*-1
		if val.timeOutNum == DeadPings {
			delete(vs.idleServersMap, key)
			log.Println("idle fail:" + val.addr + ", currentView:" + vs.currentView.String())
		}
	}
}

//
// tell the server to shut itself down.
// for testing.
// please don't change these two functions.
//
func (vs *ViewServer) Kill() {
	atomic.StoreInt32(&vs.dead, 1)
	vs.l.Close()
}

//
// has this server been asked to shut down?
//
func (vs *ViewServer) isdead() bool {
	return atomic.LoadInt32(&vs.dead) != 0
}

// please don't change this function.
func (vs *ViewServer) GetRPCCount() int32 {
	return atomic.LoadInt32(&vs.rpccount)
}

func StartServer(me string) *ViewServer {
	vs := new(ViewServer)
	vs.me = me
	// Your vs.* initializations here.
	vs.currentView = NewView(0, "", "")
	vs.status = VS_NO_PRI
	vs.idleServersMap = make(map[string]*PingServer)

	// tell net/rpc about our RPC server and handlers.
	rpcs := rpc.NewServer()
	rpcs.Register(vs)

	// prepare to receive connections from clients.
	// change "unix" to "tcp" to use over a network.
	os.Remove(vs.me) // only needed for "unix"
	l, e := net.Listen("unix", vs.me)
	if e != nil {
		log.Fatal("listen error: ", e)
	}
	vs.l = l

	// please don't change any of the following code,
	// or do anything to subvert it.

	// create a thread to accept RPC connections from clients.
	go func() {
		for vs.isdead() == false {
			conn, err := vs.l.Accept()
			if err == nil && vs.isdead() == false {
				atomic.AddInt32(&vs.rpccount, 1)
				go rpcs.ServeConn(conn)
			} else if err == nil {
				conn.Close()
			}
			if err != nil && vs.isdead() == false {
				fmt.Printf("ViewServer(%v) accept: %v\n", me, err.Error())
				vs.Kill()
			}
		}
	}()

	// create a thread to call tick() periodically.
	go func() {
		for vs.isdead() == false {
			vs.tick()
			time.Sleep(PingInterval)
		}
	}()

	return vs
}
