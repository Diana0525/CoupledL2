module ProbeQueue(
  input          clock,
  input          reset,
  output         io_sinkB_ready, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  input          io_sinkB_valid, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  input  [2:0]   io_sinkB_bits_opcode, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  input  [1:0]   io_sinkB_bits_param, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  input  [2:0]   io_sinkB_bits_size, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  input  [4:0]   io_sinkB_bits_source, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  input  [15:0]  io_sinkB_bits_address, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  input  [31:0]  io_sinkB_bits_mask, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  input  [255:0] io_sinkB_bits_data, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  input          io_sinkB_bits_corrupt, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  input          io_arb_busy_s0, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  output         io_prb_bits_s0_valid, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  output [2:0]   io_prb_bits_s0_bits_opcode, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  output [1:0]   io_prb_bits_s0_bits_param, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  output [2:0]   io_prb_bits_s0_bits_size, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  output [4:0]   io_prb_bits_s0_bits_source, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  output [15:0]  io_prb_bits_s0_bits_address, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  output [31:0]  io_prb_bits_s0_bits_mask, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  output [255:0] io_prb_bits_s0_bits_data, // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
  output         io_prb_bits_s0_bits_corrupt // @[src/main/scala/coupledL2/ProbeQueue.scala 27:14]
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [255:0] _RAND_11;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_14;
  reg [31:0] _RAND_15;
  reg [31:0] _RAND_16;
  reg [31:0] _RAND_17;
  reg [31:0] _RAND_18;
  reg [255:0] _RAND_19;
  reg [31:0] _RAND_20;
  reg [31:0] _RAND_21;
  reg [31:0] _RAND_22;
  reg [31:0] _RAND_23;
  reg [31:0] _RAND_24;
  reg [31:0] _RAND_25;
  reg [31:0] _RAND_26;
  reg [255:0] _RAND_27;
  reg [31:0] _RAND_28;
  reg [31:0] _RAND_29;
  reg [31:0] _RAND_30;
  reg [31:0] _RAND_31;
  reg [31:0] _RAND_32;
  reg [31:0] _RAND_33;
  reg [31:0] _RAND_34;
  reg [255:0] _RAND_35;
  reg [31:0] _RAND_36;
  reg [31:0] _RAND_37;
  reg [31:0] _RAND_38;
  reg [31:0] _RAND_39;
  reg [31:0] _RAND_40;
  reg [31:0] _RAND_41;
  reg [31:0] _RAND_42;
  reg [255:0] _RAND_43;
  reg [31:0] _RAND_44;
  reg [31:0] _RAND_45;
  reg [31:0] _RAND_46;
  reg [31:0] _RAND_47;
  reg [31:0] _RAND_48;
  reg [31:0] _RAND_49;
  reg [31:0] _RAND_50;
  reg [31:0] _RAND_51;
  reg [255:0] _RAND_52;
  reg [31:0] _RAND_53;
  reg [31:0] _RAND_54;
  reg [31:0] _RAND_55;
  reg [31:0] _RAND_56;
  reg [31:0] _RAND_57;
  reg [31:0] _RAND_58;
`endif // RANDOMIZE_REG_INIT
  reg  prbq_valid_reg_0; // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
  reg  prbq_valid_reg_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
  reg  prbq_valid_reg_2; // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
  reg  prbq_valid_reg_3; // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
  reg  prbq_valid_reg_4; // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
  reg [2:0] prbq_bits_reg_0_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [1:0] prbq_bits_reg_0_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [2:0] prbq_bits_reg_0_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [4:0] prbq_bits_reg_0_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [15:0] prbq_bits_reg_0_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [31:0] prbq_bits_reg_0_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [255:0] prbq_bits_reg_0_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg  prbq_bits_reg_0_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [2:0] prbq_bits_reg_1_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [1:0] prbq_bits_reg_1_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [2:0] prbq_bits_reg_1_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [4:0] prbq_bits_reg_1_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [15:0] prbq_bits_reg_1_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [31:0] prbq_bits_reg_1_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [255:0] prbq_bits_reg_1_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg  prbq_bits_reg_1_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [2:0] prbq_bits_reg_2_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [1:0] prbq_bits_reg_2_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [2:0] prbq_bits_reg_2_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [4:0] prbq_bits_reg_2_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [15:0] prbq_bits_reg_2_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [31:0] prbq_bits_reg_2_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [255:0] prbq_bits_reg_2_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg  prbq_bits_reg_2_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [2:0] prbq_bits_reg_3_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [1:0] prbq_bits_reg_3_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [2:0] prbq_bits_reg_3_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [4:0] prbq_bits_reg_3_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [15:0] prbq_bits_reg_3_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [31:0] prbq_bits_reg_3_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [255:0] prbq_bits_reg_3_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg  prbq_bits_reg_3_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [2:0] prbq_bits_reg_4_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [1:0] prbq_bits_reg_4_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [2:0] prbq_bits_reg_4_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [4:0] prbq_bits_reg_4_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [15:0] prbq_bits_reg_4_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [31:0] prbq_bits_reg_4_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg [255:0] prbq_bits_reg_4_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg  prbq_bits_reg_4_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
  reg  prb_valid_s0_reg; // @[src/main/scala/coupledL2/ProbeQueue.scala 38:33]
  reg [2:0] prb_bits_s0_reg_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
  reg [1:0] prb_bits_s0_reg_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
  reg [2:0] prb_bits_s0_reg_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
  reg [4:0] prb_bits_s0_reg_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
  reg [15:0] prb_bits_s0_reg_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
  reg [31:0] prb_bits_s0_reg_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
  reg [255:0] prb_bits_s0_reg_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
  reg  prb_bits_s0_reg_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
  reg [4:0] prbq_older_arr_0; // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
  reg [4:0] prbq_older_arr_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
  reg [4:0] prbq_older_arr_2; // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
  reg [4:0] prbq_older_arr_3; // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
  reg [4:0] prbq_older_arr_4; // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
  wire  prbq_in_sel_0 = ~prbq_valid_reg_0; // @[src/main/scala/coupledL2/ProbeQueue.scala 52:25]
  wire  prbq_alloc_0 = io_sinkB_valid & prbq_in_sel_0; // @[src/main/scala/coupledL2/ProbeQueue.scala 56:37]
  wire [4:0] _prbq_out_sel_0_T = {prbq_valid_reg_4,prbq_valid_reg_3,prbq_valid_reg_2,prbq_valid_reg_1,prbq_valid_reg_0}; // @[src/main/scala/coupledL2/ProbeQueue.scala 59:41]
  wire [4:0] _prbq_out_sel_0_T_1 = _prbq_out_sel_0_T & prbq_older_arr_0; // @[src/main/scala/coupledL2/ProbeQueue.scala 59:48]
  wire  prbq_out_sel_0 = ~(|_prbq_out_sel_0_T_1); // @[src/main/scala/coupledL2/ProbeQueue.scala 59:24]
  wire  _prbq_free_0_T_1 = ~io_arb_busy_s0; // @[src/main/scala/coupledL2/ProbeQueue.scala 60:63]
  wire  _prbq_free_0_T_2 = ~prb_valid_s0_reg; // @[src/main/scala/coupledL2/ProbeQueue.scala 60:81]
  wire  prbq_free_0 = prbq_valid_reg_0 & prbq_out_sel_0 & (~io_arb_busy_s0 | ~prb_valid_s0_reg); // @[src/main/scala/coupledL2/ProbeQueue.scala 60:60]
  wire  prbq_in_sel_1 = ~prbq_valid_reg_1 & &_prbq_out_sel_0_T[0]; // @[src/main/scala/coupledL2/ProbeQueue.scala 54:44]
  wire  prbq_alloc_1 = io_sinkB_valid & prbq_in_sel_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 56:37]
  wire  prbq_in_sel_4 = ~prbq_valid_reg_4 & &_prbq_out_sel_0_T[3:0]; // @[src/main/scala/coupledL2/ProbeQueue.scala 54:44]
  wire  prbq_alloc_4 = io_sinkB_valid & prbq_in_sel_4; // @[src/main/scala/coupledL2/ProbeQueue.scala 56:37]
  wire  prbq_in_sel_3 = ~prbq_valid_reg_3 & &_prbq_out_sel_0_T[2:0]; // @[src/main/scala/coupledL2/ProbeQueue.scala 54:44]
  wire  prbq_alloc_3 = io_sinkB_valid & prbq_in_sel_3; // @[src/main/scala/coupledL2/ProbeQueue.scala 56:37]
  wire  prbq_in_sel_2 = ~prbq_valid_reg_2 & &_prbq_out_sel_0_T[1:0]; // @[src/main/scala/coupledL2/ProbeQueue.scala 54:44]
  wire  prbq_alloc_2 = io_sinkB_valid & prbq_in_sel_2; // @[src/main/scala/coupledL2/ProbeQueue.scala 56:37]
  wire [4:0] _prbq_older_arr_0_T = {prbq_alloc_4,prbq_alloc_3,prbq_alloc_2,prbq_alloc_1,prbq_alloc_0}; // @[src/main/scala/coupledL2/ProbeQueue.scala 72:40]
  wire [4:0] _prbq_older_arr_0_T_1 = ~_prbq_older_arr_0_T; // @[src/main/scala/coupledL2/ProbeQueue.scala 72:28]
  wire [4:0] _prbq_older_arr_0_T_4 = prbq_older_arr_0 & _prbq_older_arr_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 75:46]
  wire [4:0] _prbq_out_sel_1_T_1 = _prbq_out_sel_0_T & prbq_older_arr_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 59:48]
  wire  prbq_out_sel_1 = ~(|_prbq_out_sel_1_T_1); // @[src/main/scala/coupledL2/ProbeQueue.scala 59:24]
  wire  prbq_free_1 = prbq_valid_reg_1 & prbq_out_sel_1 & (~io_arb_busy_s0 | ~prb_valid_s0_reg); // @[src/main/scala/coupledL2/ProbeQueue.scala 60:60]
  wire [4:0] _prbq_older_arr_1_T_4 = prbq_older_arr_1 & _prbq_older_arr_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 75:46]
  wire [4:0] _prbq_out_sel_2_T_1 = _prbq_out_sel_0_T & prbq_older_arr_2; // @[src/main/scala/coupledL2/ProbeQueue.scala 59:48]
  wire  prbq_out_sel_2 = ~(|_prbq_out_sel_2_T_1); // @[src/main/scala/coupledL2/ProbeQueue.scala 59:24]
  wire  prbq_free_2 = prbq_valid_reg_2 & prbq_out_sel_2 & (~io_arb_busy_s0 | ~prb_valid_s0_reg); // @[src/main/scala/coupledL2/ProbeQueue.scala 60:60]
  wire [4:0] _prbq_older_arr_2_T_4 = prbq_older_arr_2 & _prbq_older_arr_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 75:46]
  wire [4:0] _prbq_out_sel_3_T_1 = _prbq_out_sel_0_T & prbq_older_arr_3; // @[src/main/scala/coupledL2/ProbeQueue.scala 59:48]
  wire  prbq_out_sel_3 = ~(|_prbq_out_sel_3_T_1); // @[src/main/scala/coupledL2/ProbeQueue.scala 59:24]
  wire  prbq_free_3 = prbq_valid_reg_3 & prbq_out_sel_3 & (~io_arb_busy_s0 | ~prb_valid_s0_reg); // @[src/main/scala/coupledL2/ProbeQueue.scala 60:60]
  wire [4:0] _prbq_older_arr_3_T_4 = prbq_older_arr_3 & _prbq_older_arr_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 75:46]
  wire [4:0] _prbq_out_sel_4_T_1 = _prbq_out_sel_0_T & prbq_older_arr_4; // @[src/main/scala/coupledL2/ProbeQueue.scala 59:48]
  wire  prbq_out_sel_4 = ~(|_prbq_out_sel_4_T_1); // @[src/main/scala/coupledL2/ProbeQueue.scala 59:24]
  wire  prbq_free_4 = prbq_valid_reg_4 & prbq_out_sel_4 & (~io_arb_busy_s0 | ~prb_valid_s0_reg); // @[src/main/scala/coupledL2/ProbeQueue.scala 60:60]
  wire [4:0] _prbq_older_arr_4_T_4 = prbq_older_arr_4 & _prbq_older_arr_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 75:46]
  wire  _prb_valid_s0_en_T_2 = |_prbq_out_sel_0_T; // @[src/main/scala/coupledL2/ProbeQueue.scala 79:68]
  wire  _prb_valid_s0_en_T_3 = _prbq_free_0_T_2 & |_prbq_out_sel_0_T; // @[src/main/scala/coupledL2/ProbeQueue.scala 79:44]
  wire  _prb_valid_s0_en_T_9 = prb_valid_s0_reg & ~_prb_valid_s0_en_T_2 & _prbq_free_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 80:72]
  wire  prb_valid_s0_en = _prbq_free_0_T_2 & |_prbq_out_sel_0_T | _prb_valid_s0_en_T_9; // @[src/main/scala/coupledL2/ProbeQueue.scala 79:73]
  wire  _prb_bits_s0_en_T_7 = _prb_valid_s0_en_T_2 & _prbq_free_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 87:51]
  wire  prb_bits_s0_en = _prb_valid_s0_en_T_2 & _prbq_free_0_T_2 | _prb_bits_s0_en_T_7; // @[src/main/scala/coupledL2/ProbeQueue.scala 86:72]
  wire [317:0] _prb_bits_s0_in_T = {prbq_bits_reg_0_opcode,prbq_bits_reg_0_param,prbq_bits_reg_0_size,
    prbq_bits_reg_0_source,prbq_bits_reg_0_address,prbq_bits_reg_0_mask,prbq_bits_reg_0_data,prbq_bits_reg_0_corrupt}; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:74]
  wire [317:0] _prb_bits_s0_in_T_2 = prbq_free_0 ? 318'h3fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
     : 318'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:87]
  wire [317:0] _prb_bits_s0_in_T_3 = _prb_bits_s0_in_T & _prb_bits_s0_in_T_2; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:81]
  wire [317:0] _prb_bits_s0_in_T_4 = {prbq_bits_reg_1_opcode,prbq_bits_reg_1_param,prbq_bits_reg_1_size,
    prbq_bits_reg_1_source,prbq_bits_reg_1_address,prbq_bits_reg_1_mask,prbq_bits_reg_1_data,prbq_bits_reg_1_corrupt}; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:74]
  wire [317:0] _prb_bits_s0_in_T_6 = prbq_free_1 ? 318'h3fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
     : 318'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:87]
  wire [317:0] _prb_bits_s0_in_T_7 = _prb_bits_s0_in_T_4 & _prb_bits_s0_in_T_6; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:81]
  wire [317:0] _prb_bits_s0_in_T_8 = {prbq_bits_reg_2_opcode,prbq_bits_reg_2_param,prbq_bits_reg_2_size,
    prbq_bits_reg_2_source,prbq_bits_reg_2_address,prbq_bits_reg_2_mask,prbq_bits_reg_2_data,prbq_bits_reg_2_corrupt}; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:74]
  wire [317:0] _prb_bits_s0_in_T_10 = prbq_free_2 ? 318'h3fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
     : 318'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:87]
  wire [317:0] _prb_bits_s0_in_T_11 = _prb_bits_s0_in_T_8 & _prb_bits_s0_in_T_10; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:81]
  wire [317:0] _prb_bits_s0_in_T_12 = {prbq_bits_reg_3_opcode,prbq_bits_reg_3_param,prbq_bits_reg_3_size,
    prbq_bits_reg_3_source,prbq_bits_reg_3_address,prbq_bits_reg_3_mask,prbq_bits_reg_3_data,prbq_bits_reg_3_corrupt}; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:74]
  wire [317:0] _prb_bits_s0_in_T_14 = prbq_free_3 ? 318'h3fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
     : 318'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:87]
  wire [317:0] _prb_bits_s0_in_T_15 = _prb_bits_s0_in_T_12 & _prb_bits_s0_in_T_14; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:81]
  wire [317:0] _prb_bits_s0_in_T_16 = {prbq_bits_reg_4_opcode,prbq_bits_reg_4_param,prbq_bits_reg_4_size,
    prbq_bits_reg_4_source,prbq_bits_reg_4_address,prbq_bits_reg_4_mask,prbq_bits_reg_4_data,prbq_bits_reg_4_corrupt}; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:74]
  wire [317:0] _prb_bits_s0_in_T_18 = prbq_free_4 ? 318'h3fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff
     : 318'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:87]
  wire [317:0] _prb_bits_s0_in_T_19 = _prb_bits_s0_in_T_16 & _prb_bits_s0_in_T_18; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:81]
  wire [317:0] _prb_bits_s0_in_T_20 = _prb_bits_s0_in_T_3 | _prb_bits_s0_in_T_7; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:137]
  wire [317:0] _prb_bits_s0_in_T_21 = _prb_bits_s0_in_T_20 | _prb_bits_s0_in_T_11; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:137]
  wire [317:0] _prb_bits_s0_in_T_22 = _prb_bits_s0_in_T_21 | _prb_bits_s0_in_T_15; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:137]
  wire [317:0] prb_bits_s0_in = _prb_bits_s0_in_T_22 | _prb_bits_s0_in_T_19; // @[src/main/scala/coupledL2/ProbeQueue.scala 89:137]
  assign io_sinkB_ready = ~(&_prbq_out_sel_0_T); // @[src/main/scala/coupledL2/ProbeQueue.scala 94:21]
  assign io_prb_bits_s0_valid = prb_valid_s0_reg; // @[src/main/scala/coupledL2/ProbeQueue.scala 96:24]
  assign io_prb_bits_s0_bits_opcode = prb_bits_s0_reg_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 97:24]
  assign io_prb_bits_s0_bits_param = prb_bits_s0_reg_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 97:24]
  assign io_prb_bits_s0_bits_size = prb_bits_s0_reg_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 97:24]
  assign io_prb_bits_s0_bits_source = prb_bits_s0_reg_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 97:24]
  assign io_prb_bits_s0_bits_address = prb_bits_s0_reg_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 97:24]
  assign io_prb_bits_s0_bits_mask = prb_bits_s0_reg_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 97:24]
  assign io_prb_bits_s0_bits_data = prb_bits_s0_reg_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 97:24]
  assign io_prb_bits_s0_bits_corrupt = prb_bits_s0_reg_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 97:24]
  always @(posedge clock) begin
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
      prbq_valid_reg_0 <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
    end else if (prbq_alloc_0) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_valid_reg_0 <= io_sinkB_valid; // @[src/main/scala/coupledL2/ProbeQueue.scala 63:25]
    end else if (prbq_free_0) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 66:29]
      prbq_valid_reg_0 <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 67:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
      prbq_valid_reg_1 <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
    end else if (prbq_alloc_1) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_valid_reg_1 <= io_sinkB_valid; // @[src/main/scala/coupledL2/ProbeQueue.scala 63:25]
    end else if (prbq_free_1) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 66:29]
      prbq_valid_reg_1 <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 67:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
      prbq_valid_reg_2 <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
    end else if (prbq_alloc_2) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_valid_reg_2 <= io_sinkB_valid; // @[src/main/scala/coupledL2/ProbeQueue.scala 63:25]
    end else if (prbq_free_2) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 66:29]
      prbq_valid_reg_2 <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 67:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
      prbq_valid_reg_3 <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
    end else if (prbq_alloc_3) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_valid_reg_3 <= io_sinkB_valid; // @[src/main/scala/coupledL2/ProbeQueue.scala 63:25]
    end else if (prbq_free_3) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 66:29]
      prbq_valid_reg_3 <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 67:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
      prbq_valid_reg_4 <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 36:33]
    end else if (prbq_alloc_4) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_valid_reg_4 <= io_sinkB_valid; // @[src/main/scala/coupledL2/ProbeQueue.scala 63:25]
    end else if (prbq_free_4) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 66:29]
      prbq_valid_reg_4 <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 67:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_0_opcode <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_0) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_0_opcode <= io_sinkB_bits_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_0_param <= 2'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_0) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_0_param <= io_sinkB_bits_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_0_size <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_0) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_0_size <= io_sinkB_bits_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_0_source <= 5'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_0) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_0_source <= io_sinkB_bits_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_0_address <= 16'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_0) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_0_address <= io_sinkB_bits_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_0_mask <= 32'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_0) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_0_mask <= io_sinkB_bits_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_0_data <= 256'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_0) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_0_data <= io_sinkB_bits_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_0_corrupt <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_0) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_0_corrupt <= io_sinkB_bits_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_1_opcode <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_1) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_1_opcode <= io_sinkB_bits_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_1_param <= 2'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_1) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_1_param <= io_sinkB_bits_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_1_size <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_1) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_1_size <= io_sinkB_bits_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_1_source <= 5'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_1) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_1_source <= io_sinkB_bits_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_1_address <= 16'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_1) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_1_address <= io_sinkB_bits_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_1_mask <= 32'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_1) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_1_mask <= io_sinkB_bits_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_1_data <= 256'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_1) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_1_data <= io_sinkB_bits_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_1_corrupt <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_1) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_1_corrupt <= io_sinkB_bits_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_2_opcode <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_2) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_2_opcode <= io_sinkB_bits_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_2_param <= 2'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_2) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_2_param <= io_sinkB_bits_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_2_size <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_2) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_2_size <= io_sinkB_bits_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_2_source <= 5'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_2) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_2_source <= io_sinkB_bits_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_2_address <= 16'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_2) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_2_address <= io_sinkB_bits_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_2_mask <= 32'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_2) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_2_mask <= io_sinkB_bits_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_2_data <= 256'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_2) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_2_data <= io_sinkB_bits_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_2_corrupt <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_2) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_2_corrupt <= io_sinkB_bits_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_3_opcode <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_3) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_3_opcode <= io_sinkB_bits_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_3_param <= 2'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_3) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_3_param <= io_sinkB_bits_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_3_size <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_3) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_3_size <= io_sinkB_bits_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_3_source <= 5'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_3) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_3_source <= io_sinkB_bits_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_3_address <= 16'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_3) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_3_address <= io_sinkB_bits_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_3_mask <= 32'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_3) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_3_mask <= io_sinkB_bits_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_3_data <= 256'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_3) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_3_data <= io_sinkB_bits_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_3_corrupt <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_3) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_3_corrupt <= io_sinkB_bits_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_4_opcode <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_4) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_4_opcode <= io_sinkB_bits_opcode; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_4_param <= 2'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_4) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_4_param <= io_sinkB_bits_param; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_4_size <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_4) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_4_size <= io_sinkB_bits_size; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_4_source <= 5'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_4) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_4_source <= io_sinkB_bits_source; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_4_address <= 16'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_4) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_4_address <= io_sinkB_bits_address; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_4_mask <= 32'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_4) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_4_mask <= io_sinkB_bits_mask; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_4_data <= 256'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_4) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_4_data <= io_sinkB_bits_data; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
      prbq_bits_reg_4_corrupt <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 37:33]
    end else if (prbq_alloc_4) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 62:25]
      prbq_bits_reg_4_corrupt <= io_sinkB_bits_corrupt; // @[src/main/scala/coupledL2/ProbeQueue.scala 64:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 38:33]
      prb_valid_s0_reg <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 38:33]
    end else if (prb_valid_s0_en) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 82:25]
      prb_valid_s0_reg <= _prb_valid_s0_en_T_3; // @[src/main/scala/coupledL2/ProbeQueue.scala 83:22]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
      prb_bits_s0_reg_opcode <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
    end else if (prb_bits_s0_en) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 90:24]
      prb_bits_s0_reg_opcode <= prb_bits_s0_in[317:315]; // @[src/main/scala/coupledL2/ProbeQueue.scala 91:21]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
      prb_bits_s0_reg_param <= 2'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
    end else if (prb_bits_s0_en) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 90:24]
      prb_bits_s0_reg_param <= prb_bits_s0_in[314:313]; // @[src/main/scala/coupledL2/ProbeQueue.scala 91:21]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
      prb_bits_s0_reg_size <= 3'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
    end else if (prb_bits_s0_en) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 90:24]
      prb_bits_s0_reg_size <= prb_bits_s0_in[312:310]; // @[src/main/scala/coupledL2/ProbeQueue.scala 91:21]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
      prb_bits_s0_reg_source <= 5'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
    end else if (prb_bits_s0_en) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 90:24]
      prb_bits_s0_reg_source <= prb_bits_s0_in[309:305]; // @[src/main/scala/coupledL2/ProbeQueue.scala 91:21]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
      prb_bits_s0_reg_address <= 16'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
    end else if (prb_bits_s0_en) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 90:24]
      prb_bits_s0_reg_address <= prb_bits_s0_in[304:289]; // @[src/main/scala/coupledL2/ProbeQueue.scala 91:21]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
      prb_bits_s0_reg_mask <= 32'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
    end else if (prb_bits_s0_en) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 90:24]
      prb_bits_s0_reg_mask <= prb_bits_s0_in[288:257]; // @[src/main/scala/coupledL2/ProbeQueue.scala 91:21]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
      prb_bits_s0_reg_data <= 256'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
    end else if (prb_bits_s0_en) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 90:24]
      prb_bits_s0_reg_data <= prb_bits_s0_in[256:1]; // @[src/main/scala/coupledL2/ProbeQueue.scala 91:21]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
      prb_bits_s0_reg_corrupt <= 1'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 39:33]
    end else if (prb_bits_s0_en) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 90:24]
      prb_bits_s0_reg_corrupt <= prb_bits_s0_in[0]; // @[src/main/scala/coupledL2/ProbeQueue.scala 91:21]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
      prbq_older_arr_0 <= 5'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
    end else if (prbq_alloc_0) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 71:25]
      prbq_older_arr_0 <= _prbq_older_arr_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 72:25]
    end else if (|_prbq_older_arr_0_T) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 74:38]
      prbq_older_arr_0 <= _prbq_older_arr_0_T_4; // @[src/main/scala/coupledL2/ProbeQueue.scala 75:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
      prbq_older_arr_1 <= 5'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
    end else if (prbq_alloc_1) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 71:25]
      prbq_older_arr_1 <= _prbq_older_arr_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 72:25]
    end else if (|_prbq_older_arr_0_T) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 74:38]
      prbq_older_arr_1 <= _prbq_older_arr_1_T_4; // @[src/main/scala/coupledL2/ProbeQueue.scala 75:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
      prbq_older_arr_2 <= 5'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
    end else if (prbq_alloc_2) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 71:25]
      prbq_older_arr_2 <= _prbq_older_arr_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 72:25]
    end else if (|_prbq_older_arr_0_T) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 74:38]
      prbq_older_arr_2 <= _prbq_older_arr_2_T_4; // @[src/main/scala/coupledL2/ProbeQueue.scala 75:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
      prbq_older_arr_3 <= 5'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
    end else if (prbq_alloc_3) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 71:25]
      prbq_older_arr_3 <= _prbq_older_arr_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 72:25]
    end else if (|_prbq_older_arr_0_T) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 74:38]
      prbq_older_arr_3 <= _prbq_older_arr_3_T_4; // @[src/main/scala/coupledL2/ProbeQueue.scala 75:25]
    end
    if (reset) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
      prbq_older_arr_4 <= 5'h0; // @[src/main/scala/coupledL2/ProbeQueue.scala 41:33]
    end else if (prbq_alloc_4) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 71:25]
      prbq_older_arr_4 <= _prbq_older_arr_0_T_1; // @[src/main/scala/coupledL2/ProbeQueue.scala 72:25]
    end else if (|_prbq_older_arr_0_T) begin // @[src/main/scala/coupledL2/ProbeQueue.scala 74:38]
      prbq_older_arr_4 <= _prbq_older_arr_4_T_4; // @[src/main/scala/coupledL2/ProbeQueue.scala 75:25]
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  prbq_valid_reg_0 = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  prbq_valid_reg_1 = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  prbq_valid_reg_2 = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  prbq_valid_reg_3 = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  prbq_valid_reg_4 = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  prbq_bits_reg_0_opcode = _RAND_5[2:0];
  _RAND_6 = {1{`RANDOM}};
  prbq_bits_reg_0_param = _RAND_6[1:0];
  _RAND_7 = {1{`RANDOM}};
  prbq_bits_reg_0_size = _RAND_7[2:0];
  _RAND_8 = {1{`RANDOM}};
  prbq_bits_reg_0_source = _RAND_8[4:0];
  _RAND_9 = {1{`RANDOM}};
  prbq_bits_reg_0_address = _RAND_9[15:0];
  _RAND_10 = {1{`RANDOM}};
  prbq_bits_reg_0_mask = _RAND_10[31:0];
  _RAND_11 = {8{`RANDOM}};
  prbq_bits_reg_0_data = _RAND_11[255:0];
  _RAND_12 = {1{`RANDOM}};
  prbq_bits_reg_0_corrupt = _RAND_12[0:0];
  _RAND_13 = {1{`RANDOM}};
  prbq_bits_reg_1_opcode = _RAND_13[2:0];
  _RAND_14 = {1{`RANDOM}};
  prbq_bits_reg_1_param = _RAND_14[1:0];
  _RAND_15 = {1{`RANDOM}};
  prbq_bits_reg_1_size = _RAND_15[2:0];
  _RAND_16 = {1{`RANDOM}};
  prbq_bits_reg_1_source = _RAND_16[4:0];
  _RAND_17 = {1{`RANDOM}};
  prbq_bits_reg_1_address = _RAND_17[15:0];
  _RAND_18 = {1{`RANDOM}};
  prbq_bits_reg_1_mask = _RAND_18[31:0];
  _RAND_19 = {8{`RANDOM}};
  prbq_bits_reg_1_data = _RAND_19[255:0];
  _RAND_20 = {1{`RANDOM}};
  prbq_bits_reg_1_corrupt = _RAND_20[0:0];
  _RAND_21 = {1{`RANDOM}};
  prbq_bits_reg_2_opcode = _RAND_21[2:0];
  _RAND_22 = {1{`RANDOM}};
  prbq_bits_reg_2_param = _RAND_22[1:0];
  _RAND_23 = {1{`RANDOM}};
  prbq_bits_reg_2_size = _RAND_23[2:0];
  _RAND_24 = {1{`RANDOM}};
  prbq_bits_reg_2_source = _RAND_24[4:0];
  _RAND_25 = {1{`RANDOM}};
  prbq_bits_reg_2_address = _RAND_25[15:0];
  _RAND_26 = {1{`RANDOM}};
  prbq_bits_reg_2_mask = _RAND_26[31:0];
  _RAND_27 = {8{`RANDOM}};
  prbq_bits_reg_2_data = _RAND_27[255:0];
  _RAND_28 = {1{`RANDOM}};
  prbq_bits_reg_2_corrupt = _RAND_28[0:0];
  _RAND_29 = {1{`RANDOM}};
  prbq_bits_reg_3_opcode = _RAND_29[2:0];
  _RAND_30 = {1{`RANDOM}};
  prbq_bits_reg_3_param = _RAND_30[1:0];
  _RAND_31 = {1{`RANDOM}};
  prbq_bits_reg_3_size = _RAND_31[2:0];
  _RAND_32 = {1{`RANDOM}};
  prbq_bits_reg_3_source = _RAND_32[4:0];
  _RAND_33 = {1{`RANDOM}};
  prbq_bits_reg_3_address = _RAND_33[15:0];
  _RAND_34 = {1{`RANDOM}};
  prbq_bits_reg_3_mask = _RAND_34[31:0];
  _RAND_35 = {8{`RANDOM}};
  prbq_bits_reg_3_data = _RAND_35[255:0];
  _RAND_36 = {1{`RANDOM}};
  prbq_bits_reg_3_corrupt = _RAND_36[0:0];
  _RAND_37 = {1{`RANDOM}};
  prbq_bits_reg_4_opcode = _RAND_37[2:0];
  _RAND_38 = {1{`RANDOM}};
  prbq_bits_reg_4_param = _RAND_38[1:0];
  _RAND_39 = {1{`RANDOM}};
  prbq_bits_reg_4_size = _RAND_39[2:0];
  _RAND_40 = {1{`RANDOM}};
  prbq_bits_reg_4_source = _RAND_40[4:0];
  _RAND_41 = {1{`RANDOM}};
  prbq_bits_reg_4_address = _RAND_41[15:0];
  _RAND_42 = {1{`RANDOM}};
  prbq_bits_reg_4_mask = _RAND_42[31:0];
  _RAND_43 = {8{`RANDOM}};
  prbq_bits_reg_4_data = _RAND_43[255:0];
  _RAND_44 = {1{`RANDOM}};
  prbq_bits_reg_4_corrupt = _RAND_44[0:0];
  _RAND_45 = {1{`RANDOM}};
  prb_valid_s0_reg = _RAND_45[0:0];
  _RAND_46 = {1{`RANDOM}};
  prb_bits_s0_reg_opcode = _RAND_46[2:0];
  _RAND_47 = {1{`RANDOM}};
  prb_bits_s0_reg_param = _RAND_47[1:0];
  _RAND_48 = {1{`RANDOM}};
  prb_bits_s0_reg_size = _RAND_48[2:0];
  _RAND_49 = {1{`RANDOM}};
  prb_bits_s0_reg_source = _RAND_49[4:0];
  _RAND_50 = {1{`RANDOM}};
  prb_bits_s0_reg_address = _RAND_50[15:0];
  _RAND_51 = {1{`RANDOM}};
  prb_bits_s0_reg_mask = _RAND_51[31:0];
  _RAND_52 = {8{`RANDOM}};
  prb_bits_s0_reg_data = _RAND_52[255:0];
  _RAND_53 = {1{`RANDOM}};
  prb_bits_s0_reg_corrupt = _RAND_53[0:0];
  _RAND_54 = {1{`RANDOM}};
  prbq_older_arr_0 = _RAND_54[4:0];
  _RAND_55 = {1{`RANDOM}};
  prbq_older_arr_1 = _RAND_55[4:0];
  _RAND_56 = {1{`RANDOM}};
  prbq_older_arr_2 = _RAND_56[4:0];
  _RAND_57 = {1{`RANDOM}};
  prbq_older_arr_3 = _RAND_57[4:0];
  _RAND_58 = {1{`RANDOM}};
  prbq_older_arr_4 = _RAND_58[4:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
