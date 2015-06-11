package viewservice

import "net"
import "net/rpc"
import "log"
import "time"
import "sync"
import "fmt"
import "strconv"
import "os"
import "container/list"
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
const {
	VS_NO_PRI = 0
	VS_PRIACK_WAIT = 1
	VS_PRIACK_RECEIVED = 2
}

type ViewServer struct {
	mu       sync.Mutex
	l        net.Listener
	dead     int32 // for testing
	rpccount int32 // for testing
	me       string

	// Your declarations here.
	currentView *View //copy on write
	primary     *PingServer
	status      int
	backup      *PingServer
	idleServers *list.List //other idle server addr
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
	log.Println("received:" + args.Me + ", vn=" + strconv.Itoa(int(args.Viewnum)))
	log.Println(test)
	vs.mu.Lock()
	defer vs.mu.Unlock()
	//判断是否viewservice在初始状态
	vn := vs.currentView.Viewnum
	if vs.currentView.Primary == "" && vs.currentView.Backup == "" {
		log.Println("viewservice init status")
		vs.currentView = NewView(vn+1, args.Me, "")
		vs.primary = NewPingServer(args.Me, 0)
	} else {
		//非初始状态
		//如果没有backup
		//TODO 状态变化
		if vs.currentView.Backup == "" {
			log.Println("viewservice init backup")
			vs.currentView = NewView(vn+1, vs.currentView.Primary, args.Me)
			vs.backup = NewPingServer(args.Me, 0)
		} else {
			log.Println("viewservice init idleServers")
			//有back，直接放到idleServers里面
			vs.idleServers.PushBack(NewPingServer(args.Me, 0))
		}
	}
	log.Println("currentView:" + vs.currentView.String())
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
	for iter := vs.idleServers.Front(); iter != nil; iter = iter.Next() {
		iter.Value.(*PingServer).addTick()
	}
	//处理tick数达到上限的server
	vn := vs.currentView.Viewnum
	if vs.primary != nil && vs.primary.timeOutNum == DeadPings {
		if vs.currentView.Backup != "" {
			if vs.idleServers.Len() != 0 {
				ids := vs.idleServers.Front().Value.(*PingServer)
				vs.idleServers.Remove(vs.idleServers.Front())
				vs.currentView = NewView(vn+1, vs.currentView.Backup, ids.addr)
			} else {
				vs.currentView = NewView(vn+1, vs.currentView.Backup, "")
			}

		} else {
			vs.currentView = NewView(vn+1, "", "")
		}
	}
	if vs.backup != nil && vs.backup.timeOutNum == DeadPings {
		if vs.idleServers.Len() != 0 {
			ids := vs.idleServers.Front().Value.(*PingServer)
			vs.idleServers.Remove(vs.idleServers.Front())
			vs.currentView = NewView(vn+1, vs.currentView.Primary, ids.addr)
		} else {
			vs.currentView = NewView(vn+1, vs.currentView.Primary, "")
		}
	}
	for iter := vs.idleServers.Front(); iter != nil; iter = iter.Next() {
		//FIXME 直接使用iter进行remove
		if iter.Value.(*PingServer).timeOutNum == DeadPings {
			vs.idleServers.Remove(iter)
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
	vs.idleServers = list.New()

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
