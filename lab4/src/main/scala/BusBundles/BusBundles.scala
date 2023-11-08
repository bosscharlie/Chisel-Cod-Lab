package bus
import chisel3._

class WishboneMatserPort extends Bundle {
    val cyc_o = Output(Bool())
    val stb_o = Output(Bool())
    val ack_i = Input(Bool())
    val adr_o = Output(UInt(32.W))
    val dat_o = Output(UInt(32.W))
    val dat_i = Input(UInt(32.W))
    val sel_o = Output(UInt(4.W))
    val we_o  = Output(Bool())
}

class WishboneSlavePort extends Bundle {
    val stb_i = Input(Bool())
    val cyc_i = Input(Bool())
    val ack_o = Output(Bool())
    val adr_i = Input(UInt(32.W))
    val we_i  = Input(Bool())
    val dat_i = Input(UInt(32.W))
    val sel_i = Input(UInt(4.W))
    val dat_o = Output(UInt(32.W))
}