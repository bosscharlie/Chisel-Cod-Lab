package bus
import chisel3._

class WbMuxMasterPort extends Bundle {
    val adr_i = Input(UInt(32.W))
    val dat_i = Input(UInt(32.W))
    val dat_o = Output(UInt(32.W))
    val we_i = Input(Bool())
    val sel_i = Input(UInt(4.W))
    val stb_i = Input(Bool())
    val ack_o = Output(Bool())
    val cyc_i = Input(Bool())
    val err_o = Output(Bool())
    val rty_o = Output(Bool())
}

class WbMuxSlavePort extends Bundle {
    val addr = Input(UInt(32.W))
    val addr_msk = Input(UInt(32.W))
    val adr_o = Output(UInt(32.W))
    val dat_i = Input(UInt(32.W))
    val dat_o = Output(UInt(32.W))
    val we_o = Output(Bool())
    val sel_o = Output(UInt(4.W))
    val stb_o = Output(Bool())
    val ack_i = Input(Bool())
    val err_i = Input(Bool())
    val rty_i = Input(Bool())
    val cyc_o = Output(Bool())
}