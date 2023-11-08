import chisel3._
import chisel3.experimental._
import bus._

class SramTester extends ExtModule(Map("ADDR_BASE" -> "32'h8000_0000",
                                        "ADDR_MASK" -> "32'h007F_FFFF")) {
    override val desiredName = s"sram_tester" 
    val clk_i = IO(Input(Clock()))
    val rst_i = IO(Input(Bool()))
    val wb = IO(new WishboneMatserPort())

    val start = IO(Input(Bool()))
    val random_seed = IO(Input(UInt(32.W)))
    val done = IO(Output(Bool()))
    val error = IO(Output(Bool()))

    val error_round = IO(Output(UInt(32.W)))
    val error_addr = IO(Output(UInt(32.W)))
    val error_read_data = IO(Output(UInt(32.W)))
    val error_expected_data = IO(Output(UInt(32.W)))
}