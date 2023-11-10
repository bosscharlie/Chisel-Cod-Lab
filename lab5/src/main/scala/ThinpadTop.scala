import chisel3._
import chisel3.experimental._
import circt.stage.ChiselStage
import bus._

// Peripherals configuration
case class ThinpadTopConfig(
    CpldUartEnable: Boolean = true,
    BaseRamEnable: Boolean = true,
    ExtRamEnable: Boolean = true,
    Uart0Enable: Boolean = true,
    FlashEnable: Boolean = false,
    Sl811Enable: Boolean = false,
    Dm9kEnable: Boolean = false,
    VideoEnable: Boolean = false
)

case class AddrMapping(
    baseramAddr : UInt = "h8000_0000".U,
    baseramMsk  : UInt = "hffc0_0000".U,
    extramAddr  : UInt = "h8040_0000".U,
    extramMsk   : UInt = "hffc0_0000".U,
    uartAddr    : UInt = "h1000_0000".U,
    uartMsk     : UInt = "hffff_0000".U
)

class ThinpadTop(config: ThinpadTopConfig, mmap: AddrMapping) extends RawModule {
    override val desiredName = s"lab5_top"
    val clk_50M = IO(Input(Clock()))
    val clk_11M0592 = IO(Input(Clock()))

    val push_btn = IO(Input(Bool()))
    val reset_btn = IO(Input(Bool()))

    val touch_btn = IO(Input(UInt(4.W)))
    val dip_sw = IO(Input(UInt(32.W)))
    val leds = IO(Output(UInt(16.W)))
    val dpy0 = IO(Output(UInt(8.W)))
    val dpy1 = IO(Output(UInt(8.W)))

    val uart = if(config.CpldUartEnable) IO(new CpldUartPort) else null
    val base = if(config.BaseRamEnable) IO(new SramPort) else null
    val ext = if(config.ExtRamEnable) IO(new SramPort) else null
    val uart0 = noPrefix{if(config.Uart0Enable) new UartPort else null}
    val flash = if(config.FlashEnable) IO(new FlashPort) else null
    val sl811 = if(config.Sl811Enable) IO(new USBPort) else null
    val dm9k = if(config.Dm9kEnable) IO(new Dm9kPort) else null
    val video = if(config.VideoEnable) IO(new VGAPort) else null

    def connectSlaveToMux(muxSlavePort: WbMuxSlavePort, slavePort: WishboneSlavePort): Unit = {
        muxSlavePort.dat_i := slavePort.dat_o
        muxSlavePort.ack_i := slavePort.ack_o
        muxSlavePort.err_i := false.B
        muxSlavePort.rty_i := false.B
        slavePort.cyc_i := muxSlavePort.cyc_o
        slavePort.stb_i := muxSlavePort.stb_o
        slavePort.adr_i := muxSlavePort.adr_o
        slavePort.dat_i := muxSlavePort.dat_o
        slavePort.we_i  := muxSlavePort.we_o
        slavePort.sel_i := muxSlavePort.sel_o
    }

    def connectMasterToMux(muxMasterPort: WbMuxMasterPort, masterPort: WishboneMatserPort): Unit = {
        masterPort.ack_i := muxMasterPort.ack_o
        masterPort.dat_i := muxMasterPort.dat_o
        muxMasterPort.adr_i := masterPort.adr_o
        muxMasterPort.dat_i := masterPort.dat_o
        muxMasterPort.we_i  := masterPort.we_o
        muxMasterPort.sel_i := masterPort.sel_o
        muxMasterPort.stb_i := masterPort.stb_o
        muxMasterPort.cyc_i := masterPort.cyc_o
    }

    dpy0 := 0.U
    dpy1 := 0.U
    leds := 0.U
    // 禁用直连串口
    uart.rdn := true.B
    uart.wrn := true.B

    val clock_gen = noPrefix{Module(new PLL())}
    clock_gen.clk_in1 := clk_50M
    clock_gen.reset := reset_btn
    val clk_10M = clock_gen.clk_out1
    val clk_20M = clock_gen.clk_out2
    val locked = clock_gen.locked

    withClock(clk_10M) {
        withReset(reset_btn) {
            val reset_of_clock10M = RegInit(false.B)
            reset_of_clock10M := Mux(locked === false.B, true.B, false.B)
            // clock domain for sys_reset
            withReset(reset_of_clock10M) {
                val controller = Module(new Controller())
                val wbMatser = Module(new WbMaster(BusConfig()))
                val sram_controller_base = Module(new SramController())
                sram_controller_base.io.sram_io <> base
                val sram_controller_ext = Module(new SramController())
                sram_controller_ext.io.sram_io <> ext
                val uart_controller = Module(new UartController())
                uart_controller.clk_i := clk_10M
                uart_controller.rst_i := reset_of_clock10M
                uart_controller.uart_rxd_i := uart0.rxd
                uart0.txd := uart_controller.uart_txd_o
                val wb_mux = noPrefix{Module(new WishboneMux())}
                wb_mux.clk := clk_10M
                wb_mux.rst := reset_of_clock10M

                // lab5 controller port
                controller.io.baseAddr := dip_sw
                controller.io.ack_i := wbMatser.io.ack_o
                controller.io.dat_i := wbMatser.io.dat_o

                // wishbone master port
                wbMatser.io.req_i := controller.io.req_o
                wbMatser.io.we_i := controller.io.we_o
                wbMatser.io.adr_i := controller.io.adr_o
                wbMatser.io.dat_i := controller.io.dat_o
                wbMatser.io.sel_i := controller.io.sel_o
                connectMasterToMux(wb_mux.wbm, wbMatser.io.wb)
                
                // connect to slave0 baseram
                wb_mux.wbs0.addr := mmap.baseramAddr
                wb_mux.wbs0.addr_msk := mmap.baseramMsk
                connectSlaveToMux(wb_mux.wbs0, sram_controller_base.io.wb)
                
                // connect to slave1 extram
                wb_mux.wbs1.addr := mmap.extramAddr
                wb_mux.wbs1.addr_msk := mmap.extramMsk
                connectSlaveToMux(wb_mux.wbs1, sram_controller_ext.io.wb)

                // connect to slave2 uart
                wb_mux.wbs2.addr := mmap.uartAddr
                wb_mux.wbs2.addr_msk := mmap.uartMsk
                connectSlaveToMux(wb_mux.wbs2, uart_controller.wb)
            }
        }
    }
}

object Main extends App {
    ChiselStage.emitSystemVerilogFile(
      new ThinpadTop(ThinpadTopConfig(), AddrMapping()),
      Array("--target-dir","generated"),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}