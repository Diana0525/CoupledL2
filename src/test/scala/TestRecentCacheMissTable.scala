package coupledL2

import chiseltest._
import freechips.rocketchip.diplomacy.LazyModule


import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import scala.collection.mutable.ArrayBuffer
import huancun.DirtyField
import coupledL2.tl2tl._
import coupledL2.prefetch._


object TestRecentCacheMissTable extends App {
    val config = new Config((_, _, _) => {
    case L2ParamKey => L2Param(
      echoField = Seq(DirtyField()),
      prefetch = Some(ACDPParameters())
    )
  })

  val top_coupledl2 = DisableMonitors(p => LazyModule(new TestTop_L2()(p)) )(config)

  val arb_args = chisel3.aop.Select.collectDeep[RecentCacheMissTable](top_coupledl2.module){
    case ds: RecentCacheMissTable =>
      ds
  }.head

  (new chisel3.stage.ChiselStage).emitVerilog(new RecentCacheMissTable()(arb_args.p))
}
/**
  * mill -i CoupledL2.test.runMain coupledL2.TestRecentCacheMissTable
  */