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
	jobQueue := make(chan *DoJobArgs, mr.nMap+mr.nReduce)

	go func() {
		for {
			args := <-jobQueue
			worker := <-mr.registerChannel
			DPrintf("processing map worker, map job number:%d \n", args.JobNumber)
			go func(worker string, doJobArgs *DoJobArgs) {
				var doJobReply DoJobReply
				success := call(worker, "Worker.DoJob", doJobArgs, &doJobReply)
				if !success {
					jobQueue <- doJobArgs
					DPrintf("worker: %s fails, JobNumber: %d redo", worker, doJobArgs.JobNumber)
				} else {
					DPrintf("map job number: %d call return \n", args.JobNumber)
					mr.registerChannel <- worker
					countDown <- 1
					DPrintf("map job number: %d write countDownChan \n", args.JobNumber)

				}
			}(worker, args)

		}
	}()

	for m := 0; m < mr.nMap; m++ {
		jobQueue <- &DoJobArgs{mr.file, Map, m, mr.nReduce}
	}

	DPrintf("waiting for map stage")
	waitForNum(countDown, mr.nMap)

	for r := 0; r < mr.nReduce; r++ {
		jobQueue <- &DoJobArgs{mr.file, Reduce, r, mr.nMap}
	}

	DPrintf("waiting for reduce stage")
	waitForNum(countDown, mr.nReduce)

	return mr.KillWorkers()
}

func waitForNum(countDownChan chan int, num int) {
	for i := 0; i < num; i++ {
		<-countDownChan
		DPrintf("countDown: %d \n", i)
	}
}
