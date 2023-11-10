import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class Controller extends Module {
    val io = IO(new Bundle{
        val baseAddr = Input(UInt(32.W))
        val req_o = Output(Bool())
        val ack_i = Input(Bool())
        val we_o = Output(Bool())
        val adr_o = Output(UInt(32.W))
        val dat_o = Output(UInt(32.W))
        val sel_o = Output(UInt(4.W))
        val dat_i = Input(UInt(32.W))
    })
    object State extends ChiselEnum {
        val IDLE = Value
        val READ_WAIT_ACTION, READ_WAIT_CHECK, READ_DATA_ACTION, READ_DATA_DONE = Value
        val WRITE_SRAM_ACTION, WRITE_SRAM_DONE = Value
        val WRITE_WAIT_ACTION, WRITE_WAIT_CHECK, WRITE_DATA_ACTION, WRITE_DATA_DONE = Value
    }
    import State._
    val stateReg = RegInit(IDLE)
    val ramAddr = RegInit(io.baseAddr)
    val uartData = RegInit(0.U)
    val uartStatusReg = RegInit(0.U)
    switch(stateReg){
        is(IDLE) { stateReg := READ_WAIT_ACTION }
        is(READ_WAIT_ACTION) {
            when(io.ack_i) {
                stateReg := READ_WAIT_CHECK
            }
        }
        is(READ_WAIT_CHECK) {
            when(uartStatusReg(0) === 1.U) {
                stateReg := READ_DATA_ACTION
            } .otherwise {
                stateReg := READ_WAIT_ACTION
            }
        }
        is(READ_DATA_ACTION) {
            when(io.ack_i){
                stateReg := READ_DATA_DONE
            }
        }
        is(READ_DATA_DONE) { stateReg := WRITE_SRAM_ACTION }
        is(WRITE_SRAM_ACTION) {
            when(io.ack_i){
                stateReg := WRITE_SRAM_DONE
            }
        }
        is(WRITE_SRAM_DONE) { stateReg := WRITE_WAIT_ACTION }
        is(WRITE_WAIT_ACTION) {
            when(io.ack_i){
                stateReg := WRITE_WAIT_CHECK
            }
        }
        is(WRITE_WAIT_CHECK) {
            when(uartStatusReg(5) === 1.U){
                stateReg := WRITE_DATA_ACTION
            } .otherwise{
                stateReg := WRITE_WAIT_ACTION
            }
        }
        is(WRITE_DATA_ACTION) {
            when(io.ack_i){
                stateReg := WRITE_DATA_DONE
            }
        }
        is(WRITE_DATA_DONE) {
            stateReg := IDLE
        }
    }
    when(stateReg === WRITE_DATA_DONE){
        ramAddr := ramAddr + 4.U
    }
    when((stateReg===READ_WAIT_ACTION || stateReg===WRITE_WAIT_CHECK) && io.ack_i){
        uartStatusReg := io.dat_i(15,8)
    }
    when(stateReg === READ_DATA_ACTION && io.ack_i){
        uartData := 0.U(24.W) ## io.dat_i(7,0)
    }
    io.req_o := stateReg === READ_WAIT_ACTION || stateReg === READ_DATA_ACTION || stateReg === WRITE_SRAM_ACTION || stateReg === WRITE_WAIT_ACTION || stateReg === WRITE_DATA_ACTION
    io.we_o := stateReg === WRITE_SRAM_ACTION || stateReg === WRITE_DATA_ACTION
    io.adr_o := 0.U
    switch(stateReg){
        is(READ_WAIT_ACTION) { io.adr_o := "h1000_0005".U }
        is(WRITE_WAIT_ACTION) { io.adr_o := "h1000_0005".U }
        is(READ_DATA_ACTION) { io.adr_o := "h1000_0000".U }
        is(WRITE_DATA_ACTION) { io.adr_o := "h1000_0000".U }
        is(WRITE_SRAM_ACTION) { io.adr_o := ramAddr }
    }
    io.sel_o := Mux(stateReg === READ_WAIT_ACTION || stateReg === WRITE_WAIT_ACTION, "b0010".U, "b0001".U)
    io.dat_o := uartData
}