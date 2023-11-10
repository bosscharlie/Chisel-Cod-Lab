import chisel3._
import chisel3.experimental._
import bus._

class WishboneMux extends ExtModule {
    override val desiredName = s"wb_mux_3"
    val clk = IO(Input(Clock()))
    val rst = IO(Input(Bool()))
    // master signals
    val wbm = IO(new WbMuxMasterPort)
    // slave0 baseram
    val wbs0 = IO(new WbMuxSlavePort)
    // slave1 extram
    val wbs1 = IO(new WbMuxSlavePort)
    // slave2 uart
    val wbs2 = IO(new WbMuxSlavePort)
}
