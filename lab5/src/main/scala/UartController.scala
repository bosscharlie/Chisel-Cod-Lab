import chisel3._
import chisel3.experimental._
import bus._

class UartController extends ExtModule(Map("CLK_FREQ" -> 10000000,
                                            "BAUD" -> 115200)) {
    override val desiredName = s"uart_controller"
    val clk_i = IO(Input(Clock()))
    val rst_i = IO(Input(Bool()))
    val uart_rxd_i = IO(Input(Bool()))
    val uart_txd_o = IO(Output(Bool()))
    val wb = IO(new WishboneSlavePort)
}