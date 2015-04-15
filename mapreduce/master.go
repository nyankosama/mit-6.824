package mapreduce

import "container/list"
import "fmt"

type WorkerInfo struct {
	address string
	//TODO
	// You can add definitions here.
}

// Clean up all workers by sending a Shutdown RPC to each one of them Collect
// the number of jobs each work has performed.
func (mr *MapReduce) KillWorkers() *list.List {
	l := list.New()
	DPrintf("killWorkers, workers len: %d \n", len(mr.Workers))
	for _, w := range mr.Workers {
		DPrintf("DoWork: shutdown %s\n", w.address)
		args := &ShutdownArgs{}
		var reply ShutdownReply
		ok := call(w.address, "Worker.Shutdown", args, &reply)
		if ok == false {
			fmt.Printf("DoWork: RPC %s shutdown error\n", w.address)
		} else {
			l.PushBack(reply.Njobs)
		}
	}
	return l
}

//start master
func (mr *MapReduce) RunMaster() *list.List {
	//* 等待新的空闲Worker注册到Master
	//* 一旦有空闲的Worker就分配map任务给这些Worker
	//* 等待map任务结束
	//* 一旦有空闲的Worker就分配reduce任务给这些Worker
	//* 等待reduce任务结束

	countDown := make(chan int)
	for m := 0; m < mr.nMap; m++ {
		worker := <-mr.registerChannel
		DPrintf("processing map worker, map job number:%d \n", m)
		go callRpcMap(worker, mr, m, countDown)
	}

	DPrintf("waiting for map stage")
	waitForNum(countDown, mr.nMap)

	for r := 0; r < mr.nReduce; r++ {
		worker := <-mr.registerChannel
		DPrintf("processing reduce worker, map job number:%d \n", r)
		go callRpcReduce(worker, mr, r, countDown)
	}

	DPrintf("waiting for reduce stage")
	waitForNum(countDown, mr.nReduce)

	return mr.KillWorkers()
}

func callRpcMap(worker string, mr *MapReduce, JobNumber int, countDown chan int) {
	doJobArgs := &DoJobArgs{mr.file, Map, JobNumber, mr.nReduce}
	var doJobReply DoJobReply
	call(worker, "Worker.DoJob", doJobArgs, &doJobReply)
	DPrintf("map job number: %d call return \n", JobNumber)
	mr.registerChannel <- worker
	countDown <- 1
	DPrintf("map job number: %d write countDownChan \n", JobNumber)
	//TODO 暂时忽略reply，这里需要处理各种异常情况
}

func callRpcReduce(worker string, mr *MapReduce, JobNumber int, countDown chan int) {
	doJobArgs := &DoJobArgs{mr.file, Reduce, JobNumber, mr.nMap}
	var doJobReply DoJobReply
	call(worker, "Worker.DoJob", doJobArgs, &doJobReply)
	mr.registerChannel <- worker
	countDown <- 1
	//TODO 暂时忽略reply，这里需要处理各种异常情况
}

func waitForNum(countDownChan chan int, num int) {
	for i := 0; i < num; i++ {
		<-countDownChan
		DPrintf("countDown: %d \n", i)
	}
}
