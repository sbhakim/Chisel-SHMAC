//**************************************************************************
// Scratchpad Memory (asynchronous)
//--------------------------------------------------------------------------
//
// Christopher Celio
// 2013 Jun 12
//
// Provides a variable number of ports to the core, and one port to the HTIF
// (host-target interface).
//
// Assumes that if the port is ready, it will be performed immediately.
// For now, don't detect write collisions.
//
// Optionally uses synchronous read (default is async). For example, a 1-stage
// processor can only ever work using asynchronous memory!

package Common
{

import Chisel._
import Node._

import Constants._

trait MemoryOpConstants 
{
   val MT_X  = Bits(0, 3)
   val MT_B  = Bits(1, 3)
   val MT_H  = Bits(2, 3)
   val MT_W  = Bits(3, 3)
   val MT_D  = Bits(4, 3)
   val MT_BU = Bits(5, 3)
   val MT_HU = Bits(6, 3)
   val MT_WU = Bits(7, 3)

   val M_X   = Bits("b0", 1)
   val M_XRD = Bits("b0", 1) // int load
   val M_XWR = Bits("b1", 1) // int store
}

// from the pov of the datapath
class MemPortIo(data_width: Int)(implicit conf: SodorConfiguration) extends Bundle 
{
   val req    = Decoupled(new MemReq(data_width))
   val resp   = (new DecoupledIO(new MemResp(data_width))).flip
  override def clone = { new MemPortIo(data_width).asInstanceOf[this.type] }
}

class MemReq(data_width: Int)(implicit conf: SodorConfiguration) extends Bundle
{
   val addr = UInt(width = conf.xprlen)
   val data = Bits(width = data_width)
   val fcn  = Bits(width = M_X.getWidth)  // memory function code
   val typ  = Bits(width = MT_X.getWidth) // memory type
   val excl = Bool()
  override def clone = { new MemReq(data_width).asInstanceOf[this.type] }
}

class MemResp(data_width: Int)(implicit conf: SodorConfiguration) extends Bundle
{
   val addr = UInt(width = conf.xprlen)
   val data = Bits(width = data_width)
   val error = Bool()
  override def clone = { new MemResp(data_width).asInstanceOf[this.type] }
}

// NOTE: the default is enormous (and may crash your computer), but is bound by
// what the fesvr expects the smallest memory size to be.  A proper fix would
// be to modify the fesvr to expect smaller sizes.
class ScratchPadMemory(num_bytes: Int = (1 << 21))(implicit conf: SodorConfiguration) extends Module
{
   val io = new Bundle
   {
      val core_ports = Vec.fill(1) { (new MemPortIo(data_width = conf.xprlen)).flip }
      val htif_port = (new MemPortIo(data_width = 64)).flip
   }


   // HTIF min packet size is 8 bytes 
   // but 32b core will access in 4 byte chunks
   // thus we will bank the scratchpad
   val num_bytes_per_line = 8
   val num_banks = 2
   val num_lines = num_bytes / num_bytes_per_line
   println("\n    Sodor Tile: creating Synchronous Scratchpad Memory of size " + num_lines*num_bytes_per_line/1024 + " kB\n")
   val data_bank0 = Mem(Bits(width = 8*num_bytes_per_line/num_banks), num_lines, seqRead = true)
   val data_bank1 = Mem(Bits(width = 8*num_bytes_per_line/num_banks), num_lines, seqRead = true)


   // constants
   val idx_lsb = log2Up(num_bytes_per_line) 
   val bank_bit = log2Up(num_bytes_per_line/num_banks) 

   val req_valid      = io.core_ports(0).req.valid
   val req_addr       = io.core_ports(0).req.bits.addr
   val req_data       = io.core_ports(0).req.bits.data
   val req_fcn        = io.core_ports(0).req.bits.fcn
   val req_typ        = io.core_ports(0).req.bits.typ
   val byte_shift_amt = io.core_ports(0).req.bits.addr(1,0)
   val bit_shift_amt  = Cat(byte_shift_amt, UInt(0,3))

   // read access
   val r_data_idx = Reg(outType=UInt())
   val r_bank_idx = Reg(outType=Bool())

   val data_idx = req_addr >> UInt(idx_lsb)
   val bank_idx = req_addr(bank_bit)
   val read_data_out = Bits()
   val rdata_out = Bits()

   val valid_reg = Reg(init=Bool(false))
   val addr_reg = Reg(UInt(width=conf.xprlen))
   val ready_reg = Reg(init=Bool(false))
   val data_reg = Reg(UInt(width=32))

   read_data_out := Mux(r_bank_idx, data_bank1(r_data_idx), data_bank0(r_data_idx))
   rdata_out     := LoadDataGen((read_data_out >> Reg(next=bit_shift_amt)), Reg(next=req_typ))

   val press1 = Reg(init=Bool(true))
   val press2 = Reg(init=Bool(false))
   val press3 = Reg(init=Bool(false))

   press3 := press2;
   press2 := press1;
   press1 := press3;

   if(true)
   {
      when (Reg(next=io.core_ports(0).resp.ready))
      {
         valid_reg := Reg(next = Reg(next = Reg(next = io.core_ports(0).req.valid && press3)))
         addr_reg := Reg(next = Reg(next = Reg(next = io.core_ports(0).req.bits.addr)))
         ready_reg := press3
         data_reg := Reg(next = Reg(next = rdata_out))

         io.core_ports(0).resp.valid := Reg(next = Reg(next = Reg(next = io.core_ports(0).req.valid && press3)))
         io.core_ports(0).resp.bits.addr := Reg(next = Reg(next = Reg(next = io.core_ports(0).req.bits.addr)))
         io.core_ports(0).req.ready := press3
         io.core_ports(0).resp.bits.data := Reg(next = Reg(next = rdata_out))
      }
         .otherwise
      {
         io.core_ports(0).resp.valid := valid_reg
         io.core_ports(0).resp.bits.addr := addr_reg
         io.core_ports(0).req.ready := ready_reg
         io.core_ports(0).resp.bits.data := data_reg
      }
   } else {
      io.core_ports(0).req.ready := Bool(true)

      when (Reg(next=io.core_ports(0).resp.ready))
      {
         valid_reg := Reg(next = io.core_ports(0).req.valid)
         addr_reg := Reg(next = io.core_ports(0).req.bits.addr)
         data_reg := rdata_out

         io.core_ports(0).resp.valid := Reg(next = io.core_ports(0).req.valid)
         io.core_ports(0).resp.bits.addr := Reg(next = io.core_ports(0).req.bits.addr)
         io.core_ports(0).resp.bits.data := rdata_out
      }
         .otherwise
      {
         io.core_ports(0).resp.valid := valid_reg
         io.core_ports(0).resp.bits.addr := addr_reg
         io.core_ports(0).resp.bits.data := data_reg
      }
   }

   // write access
   when (req_valid && req_fcn === M_XWR)
   {
      // move the wdata into position on the sub-line
      val wdata = StoreDataGen(req_data, req_typ)
      val wmask = (StoreMask(req_typ) << bit_shift_amt)(31,0)

      when (bank_idx)
      {
         data_bank1.write(data_idx, wdata, wmask)
      }
         .otherwise
      {
         data_bank0.write(data_idx, wdata, wmask)
      }
   }
      .elsewhen (req_valid && req_fcn === M_XRD)
   {
      r_data_idx := data_idx
      r_bank_idx := bank_idx
   }

   // HTIF -------
   
   io.htif_port.req.ready := Bool(true) // for now, no back pressure
   // synchronous read
   val htif_idx = Reg(UInt())
   htif_idx := io.htif_port.req.bits.addr >> UInt(idx_lsb)
   
   io.htif_port.resp.valid     := Reg(next=io.htif_port.req.valid && io.htif_port.req.bits.fcn === M_XRD)
   io.htif_port.resp.bits.data := Cat(data_bank1(htif_idx), data_bank0(htif_idx))

   when (io.htif_port.req.valid && io.htif_port.req.bits.fcn === M_XWR)
   {
      data_bank0(htif_idx) := io.htif_port.req.bits.data(31,0)
      data_bank1(htif_idx) := io.htif_port.req.bits.data(63,32)
   }

}



object StoreDataGen
{
   def apply(din: Bits, typ: Bits): UInt =
   {
      val word = (typ === MT_W) || (typ === MT_WU)
      val half = (typ === MT_H) || (typ === MT_HU)
      val byte_ = (typ === MT_B) || (typ === MT_BU)

      val dout =  Mux(byte_, Fill(4, din( 7,0)),
                  Mux(half,  Fill(2, din(15,0)),
                             din(31,0)))
      return dout
   }
}


object StoreMask
{
   def apply(sel: UInt): UInt = 
   {
      val mask = Mux(sel === MT_H || sel === MT_HU, Bits(0xffff, 32),
                 Mux(sel === MT_B || sel === MT_BU, Bits(0xff, 32),
                                                    Bits(0xffffffff, 32)))

      return mask
   }
}

//appropriately mask and sign-extend data for the core
object LoadDataGen
{
   def apply(data: Bits, typ: Bits) : Bits =
   {
      val out = Mux(typ === MT_H,  Cat(Fill(16, data(15)),  data(15,0)),
                Mux(typ === MT_HU, Cat(Fill(16, UInt(0x0)), data(15,0)),
                Mux(typ === MT_B,  Cat(Fill(24, data(7)),    data(7,0)),
                Mux(typ === MT_BU, Cat(Fill(24, UInt(0x0)), data(7,0)), 
                                    data(31,0)))))
      
      return out
   }
}

}
