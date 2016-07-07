#! /usr/bin/env python
##
## vi:ts=4:et
##
##---------------------------------------------------------------------------##
##
## This file is part of the LZO real-time data compression library.
##
## Copyright (C) 1998 Markus Franz Xaver Johannes Oberhumer
##
## The LZO library is free software; you can redistribute it and/or
## modify it under the terms of the GNU General Public License as
## published by the Free Software Foundation; either version 2 of
## the License, or (at your option) any later version.
##
## The LZO library is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with the LZO library; see the file COPYING.
## If not, write to the Free Software Foundation, Inc.,
## 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
##
## Markus F.X.J. Oberhumer
## markus.oberhumer@jk.uni-linz.ac.at
##
##---------------------------------------------------------------------------##

import sys, string
import lzo


# /***********************************************************************
# // a very simple test driver...
# ************************************************************************/

def print_modinfo():
    #print sys.modules
    mod = sys.modules['lzo']
    #print mod
    d = mod.__dict__
    for k in d.keys():
        print k, d[k]


def test(src, level = 1):
    a0 = lzo.adler32(src)
    c =  lzo.compress(src,level)
    u1 = lzo.decompress(c)
    a1 = lzo.adler32(u1)
    o =  lzo.optimize(c)
    u2 = lzo.decompress(o)
    a2 = lzo.adler32(u2)
    if cmp(src,u1) != 0 or cmp(src,u2) != 0:
        raise lzo.error, "internal error 1"
    if cmp(a0,a1) != 0 or cmp(a0,a2) != 0:
        raise lzo.error, "internal error 2"
    print "compressed %6d -> %6d" % (len(src), len(c))


def main(args):
    ## print_modinfo()

    # print version information and module documentation
    print "LZO version %s (0x%x), %s" % (lzo.LZO_VERSION_STRING, lzo.LZO_VERSION, lzo.LZO_VERSION_DATE)
    print
    print lzo.__doc__

    # compress some simple strings
    test("aaaaaaaaaaaaaaaaaaaaaaaa")
    test("abcabcabcabcabcabcabcabc")
    test("abcabcabcabcabcabcabcabc",9)
    test(" " * 131072)
    test("")
    print "Simple compression test passed."

    # force an exception
    try:
        x = lzo.decompress("xx")
    except lzo.error, msg:
        ## print msg
        pass
    else:
        print "Exception handling does NOT work !"


if __name__ == '__main__':
    main(sys.argv)

