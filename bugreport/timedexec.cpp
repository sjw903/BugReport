//--------------------------------------------------------------------------------------------------
// Description:
//  A utility that fork-execs a child process and ensures it dies within a specified timeframe.
//--------------------------------------------------------------------------------------------------

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>

#include "Babysitter.h"
#include "Utils.h"
using namespace NsBabysitter;

#define MY_LOG_TAG "timedexec"
#define USAGE "timedexec time command ..."
// TODO: Make this like "timeout" and let the user specify the signal(s) to use.
// NAME
//        timeout - run command with bounded time
//
// SYNOPSIS
//        timeout [-signal] time command ...
//
// DESCRIPTION
//        timeout  executes  a  command  and imposes an elapsed time limit.  The
//        command is run in a separate POSIX process group so that the right thing
//        happens with commands that spawn child processes.
//
//        Arguments:
//
//        -signal
//               Specify an optional signal to send to the controlled process.  By
//               default, timeout sends SIGKILL, which cannot be caught or ignored.
//               The signal must  be  provided in its numerical value.
//
//        time   The elapsed time limit in seconds after which the command is
//               terminated.
//
//        command
//               The command to be executed.
//
// DIAGNOSTICS
//        timeout's exit status is the exit status of the specified command or 1
//        in case of a usage error.

int main(int argc, char *argv[])
{
    Utils::mLogTag = MY_LOG_TAG;

    if (3 > argc) {
        fprintf(stderr, "Not enough arguments!\n");
        fprintf(stderr, USAGE "\n");
        return 1;
    }

    errno = 0;
    int timeoutSecs = strtol(argv[1], (char**)NULL, 10);
    if ((0 != errno) || (0 >= timeoutSecs)) {
        fprintf(stderr, "Must specify a positive integer for timeout!\n");
        fprintf(stderr, USAGE "\n");
        return 1;
    }
    const struct timeval timeout = {timeoutSecs, 0};

    Babysitter babysitter(MY_LOG_TAG);
    babysitter.setOrphanKillSignal(SIGQUIT);
    int dupFrom[] = {0, 1, 2, -1};
    int dupTo[] =   {0, 1, 2, -1};
    if (-1 == babysitter.createChild(argv[2], (char** const)&argv[2], dupFrom, dupTo)) {
        fprintf(stderr, "Could not create child\n");
        return -1;
    }
    if (!babysitter.watchChild(timeout)) {
        babysitter.killChild();
    }

    return (babysitter.waitChildExitStatus());
}
