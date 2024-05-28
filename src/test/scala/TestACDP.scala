// package coupledL2

// import chisel3._
// import chisel3.util._
// import org.chipsalliance.cde.config._
// import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
// import freechips.rocketchip.diplomacy._
// import freechips.rocketchip.tile.MaxHartIdBits
// import freechips.rocketchip.tilelink._
// import chisel3.experimental.SourceInfo
// import huancun._
// import coupledL2.prefetch._
// import coupledL2.tl2tl._
// import utility.{ChiselDB, FileRegisters, TLLogger}


// object TestACDP extends App {
//   // 创建 ChiselStage 实例
//   val chiselStage = new ChiselStage
//   val config = baseConfig(1).alterPartial({
//     case L2ParamKey => L2Param(
//       clientCaches = Seq(L1Param(aliasBitsOpt = Some(2))),
//       echoField = Seq(DirtyField())
//     )
//     case HCCacheParamsKey => HCCacheParameters(
//       echoField = Seq(DirtyField())
//     )
//   })
//   val cacheParams = config(L2ParamKey)

//   val xfer = TransferSizes(1, 64) 
//   val atom = TransferSizes(1, 64 / 4) 
//   val access = TransferSizes(1, 64) 
//   val idsAll = 16
  
//   val node = TLAdapterNode( 
//     clientFn = (m: TLMasterPortParameters) => TLMasterPortParameters.v2( 
//       Seq( 
//         TLMasterParameters.v2( 
//         name = "l2", 
//         supports = TLSlaveToMasterTransferSizes( probe = xfer ), 
//         sourceId = IdRange(0, idsAll) ) 
//         ), 
//         channelBytes = cacheParams.channelBytes,
//         minLatency = 1,
//         echoFields = cacheParams.echoField,
//         requestFields = cacheParams.reqField,
//         responseKeys = cacheParams.respKey
//     ), 
//     managerFn = (m: TLSlavePortParameters) => TLSlavePortParameters.v1( 
//         m.managers.map { m => m.v2copy( 
//         regionType = if (m.regionType >= RegionType.UNCACHED) RegionType.CACHED else m.regionType, 
//         supports = TLMasterToSlaveTransferSizes( 
//           acquireB = xfer, 
//           acquireT = if (m.supportsAcquireT) xfer else TransferSizes.none, 
//           arithmetic = if (m.supportsAcquireT) atom else TransferSizes.none, 
//           logical = if (m.supportsAcquireT) atom else TransferSizes.none, 
//           get = access, putFull = if (m.supportsAcquireT) access else TransferSizes.none, 
//           putPartial = if (m.supportsAcquireT) access else TransferSizes.none, 
//           hint = access 
//           ), 
//           fifoId = None ) 
//           }, 
//           beatBytes = 32,
//           minLatency = 2,
//           responseFields = cacheParams.respField,
//           requestKeys = cacheParams.reqKey,
//           endSinkId = idsAll
//         ) 
//   )

//   val pftParams: Parameters = new Config((_,_,_) => {
//       case EdgeInKey => node.in.head._2
//       case EdgeOutKey => node.out.head._2
//       case BankBitsKey => 0
//   })
//   // val top = DisableMonitors(p => LazyModule(new TL2TLCoupledL2()(p)))(pftParams)
//   // 指定顶层模块，并传递参数
//   chiselStage.execute(args, Seq(
//     ChiselGeneratorAnnotation(() => new RecentCacheMissTable()(pftParams))
//     // ChiselGeneratorAnnotation(() => top.module)
//   ))
// }