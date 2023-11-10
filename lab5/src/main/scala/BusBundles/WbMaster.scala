package bus

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class WbMaster(busconfig: BusConfig) extends Module {
    val io = IO(new Bundle{
        val req_i = Input(Bool())
        val ack_o = Output(Bool())
        val we_i = Input(Bool())
        val adr_i = Input(UInt(busconfig.addrWidth.W))
        val dat_i = Input(UInt(busconfig.dataWidth.W))
        val sel_i = Input(UInt((busconfig.dataWidth/8).W))
        val dat_o = Output(UInt(busconfig.dataWidth.W))
        val wb = new WishboneMatserPort
    })
    object State extends ChiselEnum {
        val IDLE, ACTION, DONE = Value
    }
    import State._
    val stateReg = RegInit(IDLE)
    val rdata = RegInit(0.U)
    switch(stateReg){
        is(IDLE) {
            when(io.req_i) {
                stateReg := ACTION
            }
        }
        is(ACTION) {
            when(io.wb.ack_i){
                stateReg := DONE
            }
        }
        is(DONE) { stateReg := IDLE }
    }
    val mask = Cat(Fill(8,io.sel_i(3)), Fill(8,io.sel_i(2)), Fill(8,io.sel_i(1)), Fill(8,io.sel_i(0)))
    when(stateReg === ACTION && io.wb.ack_i && !io.we_i){
        rdata := io.wb.dat_i & mask
    }
    io.wb.cyc_o := stateReg === ACTION
    io.wb.stb_o := stateReg === ACTION
    io.ack_o    := stateReg === DONE
    io.wb.adr_o := io.adr_i
    io.wb.dat_o := io.dat_i
    io.wb.sel_o := io.sel_i
    io.wb.we_o  := io.we_i
    io.dat_o    := rdata
}