package counter

import chisel3._
import circt.stage.ChiselStage

class Counter extends Module {
    override val desiredName = s"counter"
    // val io = IO(new Bundle {
    //     val trigger = Input(Bool())
    //     val count = Output(UInt(4.W))
    // })
    val trigger = IO(Input(Bool()))
    val count = IO(Output(UInt(4.W)))

    // val triggerRisingEdge = io.trigger & RegNext(io.trigger)
    val countReg = RegInit(0.U(4.W))
    when (trigger) {
        countReg := Mux(countReg === "b1111".U, countReg, countReg+1.U)
    }
    count := countReg
}

object Main extends App {
    ChiselStage.emitSystemVerilogFile(
      new Counter,
      Array("--target-dir","generated"),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}