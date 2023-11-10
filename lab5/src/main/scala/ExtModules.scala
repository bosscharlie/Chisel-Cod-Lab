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

class UartController extends ExtModule(Map("CLK_FREQ" -> 10000000,
                                            "BAUD" -> 115200)) {
    override val desiredName = s"uart_controller"
    val clk_i = IO(Input(Clock()))
    val rst_i = IO(Input(Bool()))
    val uart_rxd_i = IO(Input(Bool()))
    val uart_txd_o = IO(Output(Bool()))
    val wb = IO(new WishboneSlavePort)
}