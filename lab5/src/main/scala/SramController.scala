import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import bus._
import chisel3.experimental.Analog

class SramController extends Module {
    val io = IO(new Bundle {
        val wb = new WishboneSlavePort()
        val sram_io = new SramPort()
    })
    object State extends ChiselEnum {
        val IDLE, READ, READ2, WRITE, WRITE2, WRITE3, DONE = Value
    }
    import State._
    val stateReg = RegInit(IDLE)
    val rdData = RegInit(0.U(32.W))
    val dataz = WireDefault(true.B)
    dataz := stateReg === READ || stateReg === READ2
    val tri = Module(new TriStateGate)
    tri.io.triData <> io.sram_io.ram_data
    tri.io.dataz := dataz
    tri.io.datain := io.wb.dat_i
    switch(stateReg) {
        is(IDLE) {
            when(io.wb.stb_i && io.wb.cyc_i) {
                when(io.wb.we_i) {
                    stateReg := WRITE
                } .otherwise {
                    stateReg := READ
                }
            }
        }
        is(READ) { stateReg := READ2 }
        is(READ2) {
            stateReg := DONE
            rdData := tri.io.dataout
        }
        is(WRITE) { stateReg := WRITE2 }
        is(WRITE2) { stateReg := WRITE3 }
        is(WRITE3) { stateReg := DONE }
        is(DONE) { stateReg := IDLE }
    }
    io.wb.ack_o := stateReg === DONE
    io.wb.dat_o := rdData
    io.sram_io.ram_addr := io.wb.adr_i(21,2)
    io.sram_io.ram_be_n := ~io.wb.sel_i
    io.sram_io.ram_ce_n := stateReg === IDLE || stateReg === DONE
    io.sram_io.ram_oe_n := stateReg =/= READ && stateReg =/= READ2
    io.sram_io.ram_we_n := stateReg =/= WRITE2 
}