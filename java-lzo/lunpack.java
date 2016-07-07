/* lunpack.java -- example program

   This file is part of the LZO real-time data compression library.

   Copyright (C) 1999 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1998 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1997 Markus Franz Xaver Johannes Oberhumer
   Copyright (C) 1996 Markus Franz Xaver Johannes Oberhumer

   The LZO library is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation; either version 2 of
   the License, or (at your option) any later version.

   The LZO library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with the LZO library; see the file COPYING.
   If not, write to the Free Software Foundation, Inc.,
   59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

   Markus F.X.J. Oberhumer
   <markus.oberhumer@jk.uni-linz.ac.at>
   http://wildsau.idv.uni-linz.ac.at/mfx/lzo.html
 */


import org.lzo.*;
import java.io.*;
// import java.util.zip.Adler32;  // @JDK@ 1.1


/***********************************************************************
 * This program serves as a test driver for the LZO1X decompressor.
 *
 * It can test and unpack files that were created with the
 * lpack example program from LZO 1.06.
 *
 * @author  Markus F.X.J. Oberhumer <markus.oberhumer@jk.uni-linz.ac.at>
 * @see     examples/lpack.c in the LZO 1.06 distribution
 ***********************************************************************/

class lunpack
{
    Lzo1xDecompressor bc = null;

    boolean opt_checksum = true;
    boolean opt_debug = false;

    // magic file header for compressed lpack files
    static final byte magic[] =
        { 0x00, (byte)0xe9, 0x4c, 0x5a, 0x4f, (byte)0xff, 0x1a };

    // for statistics
    int total_in = 0;
    int total_out = 0;
    int total_blocks = 0;
    int total_c = 0;
    int total_d = 0;

    // for benchmarks
    int compress_loops = 1;
    int decompress_loops = 1;
    long compress_time = 0;
    long decompress_time = 0;
    long total_time = 0;


    //
    // util
    //

    int xread(InputStream f, byte buf[], int off, int len, boolean allow_eof)
        throws IOException
    {
        int l = Util.xread(f,buf,off,len,allow_eof);
        total_in += l;
        return l;
    }

    void xwrite(OutputStream f, byte buf[], int off, int len)
        throws IOException
    {
        Util.xwrite(f,buf,off,len);
        total_out += len;
    }

    int xread32(InputStream f)
        throws IOException
    {
        int v = Util.xread32(f);
        total_in += 4;
        return v;
    }

    void xwrite32(OutputStream f, int v)
        throws IOException
    {
        Util.xwrite32(f,v);
        total_out += 4;
    }

    int xgetc(InputStream f)
        throws IOException
    {
        int v = Util.xgetc(f);
        total_in += 1;
        return v;
    }

    void xputc(OutputStream f, int v)
        throws IOException
    {
        Util.xputc(f,v);
        total_out += 1;
    }


    //
    // decompression
    //

    void do_decompress(InputStream fi, OutputStream fo)
        throws Exception
    {
        reset_stats();
        long total_start_time = System.currentTimeMillis();

        /*
         * Step 1: check magic header, read flags & block size, init checksum
         */
        byte m[] = new byte[magic.length];
        if (xread(fi,m,0,m.length,true) != m.length ||
            Util.memcmp(m,magic,m.length) != 0) {
            throw new DataFormatException("header error - this file is not compressed by lpack");
        }
        int flags = xread32(fi);
        int method = xgetc(fi);
        int level = xgetc(fi);
        if (method != 1) {
            throw new DataFormatException("header error - invalid method " + method);
        }
        int block_size = xread32(fi);
        if (block_size < 1024 || block_size > 1024*1024) {
            throw new DataFormatException("header error - invalid block size " + block_size);
        }
        Adler32 checksum = null;
        if (opt_checksum && (flags & 1) != 0)
            checksum = new Adler32();

        /*
         * Step 2: allocate buffers for decompression
         */
        int buf_len;
        byte in_buf[] = null;
        byte out_buf[] = null;

        if (decompress_loops == 1) {
            // we use in-place decompression to save some memory
            buf_len = block_size + block_size / 64 + 16 + 3;
            in_buf = new byte[buf_len];
            out_buf = in_buf;
        }
        else
        {
            // cannot use in-place decompression when running benchmark
            buf_len = block_size;
            in_buf = new byte[buf_len];
            out_buf = new byte[buf_len];
        }

        /*
         * Step 3: process blocks
         */
        for (;;)
        {
            int in;
            int out;
            int in_len;
            int out_len;

            /* read uncompressed size */
            out_len = xread32(fi);

            /* exit if last block (EOF marker) */
            if (out_len == 0)
                break;

            /* read compressed size */
            in_len = xread32(fi);

            /* sanity check of the size values */
            if (in_len > block_size || out_len > block_size ||
                in_len <= 0 || out_len <= 0 || in_len > out_len) {
                throw new DataFormatException("block size error - data corrupted");
            }

            /* place compressed block at the top of in_buf[] */
            in = buf_len - in_len;
            out = 0;
            xread(fi,in_buf,in,in_len,false);

            if (in_len < out_len)
            {
                /* decompress */
                int loops = decompress_loops;
                long start_time = System.currentTimeMillis();
                do {
                    Int new_len = new Int(out_len);
                    int r = bc.decompress(in_buf,in,in_len,out_buf,out,new_len);
                    if (r != bc.LZO_E_OK || new_len.intValue() != out_len) {
                        //System.out.println(r + " " + in_len + " " + out_len + " " + new_len.intValue());
                        throw new DataFormatException("compressed data violation");
                    }
                } while (--loops > 0);
                decompress_time += System.currentTimeMillis() - start_time;
                /* write decompressed block */
                xwrite(fo,out_buf,out,out_len);
                /* update checksum */
                if (checksum != null)
                    checksum.update(out_buf,out,out_len);
                total_d += out_len;
            }
            else
            {
                /* write original (incompressible) block */
                xwrite(fo,in_buf,in,in_len);
                /* update checksum */
                if (checksum != null)
                    checksum.update(in_buf,in,in_len);
                total_d += in_len;
            }

            total_c += in_len;
            total_blocks++;
        }

        /* read and verify checksum */
        if ((flags & 1) != 0) {
            long c = xread32(fi) & 0xffffffffL;
            if (checksum != null && c != checksum.getValue()) {
                throw new DataFormatException("checksum error - data corrupted");
            }
        }

        total_time += System.currentTimeMillis() - total_start_time;
    }


    //
    // stats
    //

    private void reset_stats() {
        total_in = 0;
        total_out = 0;
        total_blocks = 0;
        total_c = 0;
        total_d = 0;
        compress_time = 0;
        decompress_time = 0;
        total_time = 0;
    }

    void print_stats(String n) {
        PrintStream f = System.out;
        f.println("processing time: " + ((total_time) / 1000.0) + " seconds");
        if (compress_loops <= 1 && decompress_loops <= 1)
            return;

        float ratio = (total_d > 0) ? (100.0f * total_c) / total_d : 0;
        float c_kbs = 0;
        float d_kbs = 0;
        if (compress_time > 0.001)
            c_kbs = (1.0f * total_d * compress_loops) / compress_time;
        if (decompress_time > 0.001)
            d_kbs = (1.0f * total_d * decompress_loops) / decompress_time;

        if (decompress_time > 0.001)
            f.println("decompression speed: about " + d_kbs + " kB per second");

        // print a benchmark line for util/table.pl
        f.print("LZO1X [java] | " + n + " " + total_d);
        f.print(" " + total_blocks + " " + total_c);
        f.println(" " + ratio + " " + c_kbs + " " + d_kbs + " |");
    }


    //
    // main
    //

    static void usage(PrintStream f) {
        f.println("Usage: java lunpack -d [options] input-file output-file (decompress)");
        f.println("Usage: java lunpack -t [options] input-file...          (test)");
        f.println();
        f.println("Options:");
        f.println("  -F      do not compute a checksum (faster)");
        f.println("  -D#     run decompressor # times (benchmark)");
    }


    public static void main(String args[]) {
        int r = 0;
        int opt_block_size = 256 * 1024;
        boolean opt_decompress = false;
        boolean opt_test = false;
        String iname = null;
        String oname = null;
        FileInputStream fi = null;
        FileOutputStream fo = null;
        lunpack lp = new lunpack();
        lp.bc = new Lzo1xDecompressor();

        try {
            int i = 0;

            while (i < args.length && args[i].startsWith("-")) {
                if (args[i].equals("-d"))
                    opt_decompress = true;
                else if (args[i].equals("-t"))
                    opt_test = true;
                else if (args[i].equals("-F"))
                    lp.opt_checksum = false;
                else if (args[i].equals("--debug"))
                    lp.opt_debug = true;
                else if (args[i].startsWith("-C"))
                {
                    lp.compress_loops = 1;
                    String opt = args[i].substring(2);
                    int o = opt.length() > 0 ? Integer.parseInt(opt) : 0;
                    if (o > 0)
                        lp.compress_loops = o;
                }
                else if (args[i].startsWith("-D"))
                {
                    lp.decompress_loops = 1;
                    String opt = args[i].substring(2);
                    int o = opt.length() > 0 ? Integer.parseInt(opt) : 0;
                    if (o > 0)
                        lp.decompress_loops = o;
                }
                else if (args[i].startsWith("-b")) {
                    String opt = args[i].substring(2);
                    int o = opt.length() > 0 ? Integer.parseInt(opt) : 0;
                    if (o < 1024 || o > 1024*1024)
                        throw new IllegalArgumentException("invalid block size " + o);
                    opt_block_size = o;
                }
                else
                    throw new IllegalArgumentException("invalid option " + args[i]);
                i++;
            }

            if (opt_test) {
                if (i >= args.length)
                    throw new IllegalArgumentException();
                while (i < args.length) {
                    iname = args[i++];
                    fi = new FileInputStream(iname);
                    lp.do_decompress(fi,fo);
                    fi.close(); fi = null;
                    System.out.println(iname + ": tested ok (" + lp.total_in +
                                       " -> " + lp.total_out + " bytes)");
                    lp.print_stats(iname);
                }
            } else if (opt_decompress) {
                if (i + 2 != args.length)
                    throw new IllegalArgumentException();
                iname = args[i++];
                oname = args[i++];
                fi = new FileInputStream(iname);
                fo = new FileOutputStream(oname);
                lp.do_decompress(fi,fo);
                System.out.println("decompressed " + lp.total_in +
                                   " into " + lp.total_out + " bytes");
                lp.print_stats(iname);
            }
            else
                usage(System.err);
        } catch (IllegalArgumentException e) {
            r = 1;
            System.err.println(e);
            System.err.println();
            usage(System.err);
        } catch (Exception e) {
            r = 1;
            if (iname != null)
                System.err.print(iname + ": ");
            System.err.println(e);
        } finally {
            try {
                if (fi != null)
                    fi.close();
            } catch (Exception e) { }
            try {
                if (fo != null)
                    fo.close();
            } catch (Exception e) { }
        }

        System.exit(r);
    }
}


// vi:ts=4:et

