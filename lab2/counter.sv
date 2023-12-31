// Generated by CIRCT firtool-1.38.0
// Standard header to adapt well known macros to our needs.
`ifndef RANDOMIZE
  `ifdef RANDOMIZE_REG_INIT
    `define RANDOMIZE
  `endif // RANDOMIZE_REG_INIT
`endif // not def RANDOMIZE

// RANDOM may be set to an expression that produces a 32-bit random unsigned value.
`ifndef RANDOM
  `define RANDOM $random
`endif // not def RANDOM

// Users can define INIT_RANDOM as general code that gets injected into the
// initializer block for modules with registers.
`ifndef INIT_RANDOM
  `define INIT_RANDOM
`endif // not def INIT_RANDOM

// If using random initialization, you can also define RANDOMIZE_DELAY to
// customize the delay used, otherwise 0.002 is used.
`ifndef RANDOMIZE_DELAY
  `define RANDOMIZE_DELAY 0.002
`endif // not def RANDOMIZE_DELAY

// Define INIT_RANDOM_PROLOG_ for use in our modules below.
`ifndef INIT_RANDOM_PROLOG_
  `ifdef RANDOMIZE
    `ifdef VERILATOR
      `define INIT_RANDOM_PROLOG_ `INIT_RANDOM
    `else  // VERILATOR
      `define INIT_RANDOM_PROLOG_ `INIT_RANDOM #`RANDOMIZE_DELAY begin end
    `endif // VERILATOR
  `else  // RANDOMIZE
    `define INIT_RANDOM_PROLOG_
  `endif // RANDOMIZE
`endif // not def INIT_RANDOM_PROLOG_

module counter(	// <stdin>:3:10
  input        clock,	// <stdin>:4:11
               reset,	// <stdin>:5:11
               trigger,	// src/main/scala/counter/Counter.scala:12:21
  output [3:0] count	// src/main/scala/counter/Counter.scala:13:19
);

  reg [3:0] countReg;	// src/main/scala/counter/Counter.scala:16:27
  always @(posedge clock) begin	// <stdin>:4:11
    if (reset)	// <stdin>:4:11
      countReg <= 4'h0;	// src/main/scala/counter/Counter.scala:16:27
    else if (~trigger | (&countReg)) begin	// src/main/scala/counter/Counter.scala:16:27, :17:20, :18:{18,34}
    end
    else	// src/main/scala/counter/Counter.scala:16:27, :17:20, :18:18
      countReg <= countReg + 4'h1;	// src/main/scala/counter/Counter.scala:16:27, :18:67
  end // always @(posedge)
  `ifndef SYNTHESIS	// <stdin>:3:10
    `ifdef FIRRTL_BEFORE_INITIAL	// <stdin>:3:10
      `FIRRTL_BEFORE_INITIAL	// <stdin>:3:10
    `endif // FIRRTL_BEFORE_INITIAL
    initial begin	// <stdin>:3:10
      automatic logic [31:0] _RANDOM_0;	// <stdin>:3:10
      `ifdef INIT_RANDOM_PROLOG_	// <stdin>:3:10
        `INIT_RANDOM_PROLOG_	// <stdin>:3:10
      `endif // INIT_RANDOM_PROLOG_
      `ifdef RANDOMIZE_REG_INIT	// <stdin>:3:10
        _RANDOM_0 = `RANDOM;	// <stdin>:3:10
        countReg = _RANDOM_0[3:0];	// src/main/scala/counter/Counter.scala:16:27
      `endif // RANDOMIZE_REG_INIT
    end // initial
    `ifdef FIRRTL_AFTER_INITIAL	// <stdin>:3:10
      `FIRRTL_AFTER_INITIAL	// <stdin>:3:10
    `endif // FIRRTL_AFTER_INITIAL
  `endif // not def SYNTHESIS
  assign count = countReg;	// <stdin>:3:10, src/main/scala/counter/Counter.scala:16:27
endmodule

