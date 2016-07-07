require 5.003;
use ExtUtils::MakeMaker 5.16;
use Config;

# Change the path on the next line to point to the directory where
# <lzoconf.h> and <lzo1x.h> can be found.

#$LZO_INCLUDE = '-I/usr/include/lzo';
#$LZO_INCLUDE = '-I/usr/local/include';
#$LZO_INCLUDE = '-I/usr/local/include/lzo';

# If liblzo is not installed in some place obvious, uncomment the next
# line and change the path to point to the directory where liblzo is
# installed.

#$LZO_LIB     = '-L/usr/local/lib';


# It shouldn't be necessary to change anything from here on.

WriteMakefile(
                NAME   	 	=> 'Compress::LZO',
		VERSION_FROM  	=> 'LZO.pm',
		LIBS	 	=> [ "$LZO_LIB -llzo" ],
	      	INC      	=> "$LZO_INCLUDE",
		'dist'	 	=> {COMPRESS=>'gzip', SUFFIX=>'gz'},
	     ) ;

