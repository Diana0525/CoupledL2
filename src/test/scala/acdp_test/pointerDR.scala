// import chisel3._
// import chiseltest._
// import coupledL2.prefetch._
// import org.scalatest.flatspec.AnyFlatSpec
// import java.lang.reflect.Parameter

// class PointerDataRecognitionTests extends AnyFlatSpec with ChiselScalatestTester {
  
//   behavior of "PointerDataRecognition"

//   it should "correctly process input signals and produce outputs" in {
//     test(new PointerDataRecognition(implicitly p: Parameters)) { c =>
//         // 初始化输入信号
//         c.io.train.initSource().setSourceClock(c.clock)
//         c.io.pointerAddr.initSource().setSourceClock(c.clock)
//         c.io.test.resp.initSource().setSourceClock(c.clock)

//         //输出
//         c.io.continuousPf.initSink().setSourceClock(c.clock)
//         c.io.prefetchDisable.initSink().setSourceClock(c.clock)
//         c.io.test.req.initSink().setSourceClock(c.clock)
      
//         val inputTrain = Seq.fill(1)(new PrefetchTrain).Lit( _.tag = 0.U, _.set = 0.U, _.needT = false.U,
//         _.source = 0.U, _.vaddr = 0.U, _.hit = false.B, _.prefetched = false.B, _.pfsource = 0.U, _.reqsource = 0.U,
//         _.pfdata = 1.U, _.restartBit = false.B, _.pfDepth = 0.U, _.isContinuation = false.B)
//         val inputpointerAddr = Seq.fill(1)(0.U)
//         val inputtestResp = Seq.fill(1)(new TestMissAddressResp).Lit(_.hit = true.B)

//         val outputcontinuousPf = Seq.fill(1)(new continuousPrefetch).Lit(_.prefetchDepth = 3.U, _.restartBit = false.B)
//         val outputprefetchDisable = Seq.fill(1)(false.B)
//         val outputtestreq = Seq.fill(1)(new TestMissAddressReq).Lit(_.pfAddr = 1.U)

//         fork{
//             c.io.train.enqueueSeq(inputTrain)
//             c.io.pointerAddr.enqueueSeq(inputpointerAddr)
//             c.io.test.resp.enqueueSeq(inputtestResp)
//         }.fork{
//             for(expected <- outputcontinuousPf) {
//                 c.io.continuousPf.expectDequeue(expected)
//             }
//             for(expected <- outputprefetchDisable) {
//                 c.io.prefetchDisable.expectDequeue(expected)
//             }
//             for(expected <- outputtestreq) {
//                 c.io.test.req.expectDequeue(expected)
//             }
//         }.join
      
//     }
//   }
// }
