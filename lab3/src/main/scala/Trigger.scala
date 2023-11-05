import chisel3._
import circt.stage.ChiselStage

class Trigger extends Module {
    override val desiredName = s"trigger"
    val button = IO(Input(Bool()))
    val trigger= IO(Output(Bool()))

    val reg = RegInit(false.B)
    reg := button & !RegNext(button)
    trigger := reg
}

object TriggerMain extends App {
    ChiselStage.emitSystemVerilogFile(
      new Trigger,
      Array("--target-dir","generated"),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}