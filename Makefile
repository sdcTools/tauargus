######################################################################################
# Makefile for making needed dll's
# use "make 32BIT=true" to compile for 32 bit system (default)
# use "make 32BIT=false" to compile for 64 bit system
# use "make SOLVER=foo" to compile only for selected LP-solvers
#	    where foo s a comma separated list
#	    Allowed solvers: CP=Cplex, XP=Xpress, SC=SCIP
#	    So e.g. "make SOLVER=SC,CP" would only compile for Cplex and SCIP
######################################################################################

# In this setup all submodules are at the same directory level as tauargus
# i.e., the current Makefile is in ../tauargus
CSPDIR           = ../CSP
CRPDIR           = ../CRP
TAUHITASDIR      = ../tauhitas
TAUROUNDERDIR    = ../taurounder
LIBTAUARGUSDIR   = ../libtauargus

32BIT            = true
#32BIT            = false

####### Compiler, tools and options
ifeq ($(32BIT), false)  # 64 bit assumed
    BITS         = -m64 -D_LP64
    ARCH         = x86_64
    CND_PLATFORM = MinGW-Windows64
    JAVADIR      = D:/Peter-Paul/Documents/Java/zulu8.52.0.23-ca-jdk8.0.282-win_x64
    GNUDIR       = C:/Progra~1/mingw-w64/x86_64-8.1.0-posix-seh-rt_v6-rev0/mingw64/bin
else                    # 32 bit assumed
    BITS         = -m32
    ARCH         = x86
    CND_PLATFORM = MinGW-Windows
    JAVADIR      = D:/Peter-Paul/Documents/Java/zulu8.52.0.23-ca-jdk8.0.282-win_i686
    GNUDIR       = C:/Progra~2/mingw-w64/i686-8.1.0-win32-sjlj-rt_v6-rev0/mingw32/bin
endif

SWIGDIR          = D:/Peter-Paul/Documents/Thuiswerk/Programmatuur/swigwin-4.0.1

CC               = $(GNUDIR)/gcc
CXX              = $(GNUDIR)/g++ -gstabs
WINDRES          = $(GNUDIR)/windres
MKDIR            = mkdir
RM               = rm -f
CP               = cp -p

# Solvers
SOLVER = CP,XP,SC# default is all three
comma:=,
null:=
space:= $(null) #
ifeq (,$(SOLVER)) # if not specified, use defaults
    USEDSOLVERS = CP XP SC
else
    USEDSOLVERS = $(subst $(comma), $(space), $(SOLVER))
endif

ifneq (,$(findstring CP,$(USEDSOLVERS)))
	CSPTARGETS += CPLEX
endif
ifneq (,$(findstring XP,$(USEDSOLVERS)))
	CSPTARGETS += XPRESS
endif
ifneq (,$(findstring SC,$(USEDSOLVERS)))
	CSPTARGETS += SCIP
endif

# This will override the settings in the subsystem Makefiles
# This way the GCC and JAVA and SWIG versions can easily be set the same 
# for all subsystems while the subsystem Makefiles can be used on their own as well
MYMAKEFLAGS      = 'JAVADIR=$(JAVADIR)' 'GNUDIR=$(GNUDIR)' 'BITS=$(BITS)'\
                   'ARCH=$(ARCH)' 'CND_PLATFORM=$(CND_PLATFORM)' 'SWIGDIR=$(SWIGDIR)'\
                   'CC=$(CC)' 'CXX=$(CXX)' 'WINDRES=$(WINDRES)' 'MKDIR=$(MKDIR)'\
                   'RM=$(RM)' 'CP=$(CP)' '32BIT=$(32BIT)'

#targets
.PHONY: CSP CRP modular rounder arguslib

all: modular rounder arguslib

CSP:
	$(MAKE) $(MYMAKEFLAGS) clean $(CSPTARGETS) -C $(CSPDIR)

CRP:
	$(MAKE) $(MYMAKEFLAGS) clean all SOLVER=$(SOLVER) -C $(CRPDIR)

modular: CSP
	$(MAKE) $(MYMAKEFLAGS) clean all SOLVER=$(SOLVER) -C $(TAUHITASDIR)

rounder: CRP
	$(MAKE) $(MYMAKEFLAGS) clean all -C $(TAUROUNDERDIR)

arguslib:
	$(MAKE) $(MYMAKEFLAGS) clean all -C $(LIBTAUARGUSDIR)