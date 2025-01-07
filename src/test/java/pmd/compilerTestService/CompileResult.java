package pmd.compilerTestService;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;


public class CompileResult {

    public enum CompileStatus {
        Succeeded,
        Failed,
        Exception,
        Warning
    }

    public enum CompareResult {
        Unspecified,
        Match,
        Unmatch,
        Match_NotEqualLength,
        Match_WithoutMemo
    }

    public CompileStatus status;

    public CompileResult.CompileStatus getStatus() {
        return status;
    }

    private final int exitCode;

    public int getExitCode() {
        return exitCode;
    }

    private final byte[] compiledBinary;

    public byte[] getCompiledBinary() {
        return compiledBinary;
    }

    private final String log;

    public String getLog() {
        return log;
    }

    private final Integer memoWriteAddress;

    public int getMemoWriteAddress() {
        return memoWriteAddress;
    }

    public CompileResult(boolean succeeded, byte[] compiledBinary, String log, Integer memoWriteAddress /* = null */) {
        this(succeeded ? 0 : 1, compiledBinary, log, memoWriteAddress);
    }

    public CompileResult(int exitCode, byte[] compiledBinary, String log) {
        this(exitCode, compiledBinary, log, null);
    }

    public CompileResult(int exitCode, byte[] compiledBinary, String log, Integer memoWriteAddress /* = null */) {
        var succeeded = exitCode == 0;
        if (succeeded) {
            if (!log.contains("Warning")) {
                status = CompileStatus.Succeeded;
            } else {
                status = CompileStatus.Warning;
            }
        } else {
            if (!log.contains("Exception")) {
                status = CompileStatus.Failed;
            } else {
                status = CompileStatus.Exception;
            }
        }
        this.exitCode = exitCode;
        this.compiledBinary = compiledBinary;
        this.log = log;
        this.memoWriteAddress = memoWriteAddress < 0 ? null : memoWriteAddress;
    }

    public void writeLog(Logger logger) {
        switch (status) {
            case Failed:
                logger.log(Level.ERROR, log);
                break;
            case Exception:
                logger.log(Level.ERROR, log);
                break;
            case Warning:
                logger.log(Level.WARNING, log);
                break;
        }
    }

    public CompileResult.CompareResult compare(CompileResult target) {
        if (status == CompileStatus.Failed || target.status == CompileStatus.Failed ||
                status == CompileStatus.Exception || target.status == CompileStatus.Exception ||
                compiledBinary == null || target.compiledBinary == null) {
            return CompareResult.Unspecified;
        }
        int size = Math.min(compiledBinary.length, target.compiledBinary.length);

        for (int i = 0; i < size; i++) {
            if (compiledBinary[i] != target.compiledBinary[i]) {
                var offset = getMemoOffset(compiledBinary) != null ? memoWriteAddress != null ? target.memoWriteAddress != null ? size : 0 : 0 : 0;

                return i >= offset ? CompareResult.Match_WithoutMemo : CompareResult.Unmatch;
            }
        }

        return compiledBinary.length == target.compiledBinary.length ? CompareResult.Match : CompareResult.Match_NotEqualLength;
    }

    public static Integer getMemoOffset(byte[] array) {

            //  正攻法 (/v あり時のみ)
//        if (array.length >= 0x1a && array[1] == 0x1a) {
//            var offset = array[0x19] + array[0x1a] * 256 - 4 + 1;
//            offset = array[offset] + array[offset + 1] * 256 + 1;
//            offset = array[offset] + array[offset + 1] * 256;
//            return offset;
//        }

        if (array.length >= 4) {
            var offset = array[array.length - 4] + array[array.length - 3] * 256;
            offset++;
            while (offset < array.length - 2) {
                if (array[offset] == 0) {
                    offset++;
                    offset = array[offset] + array[offset + 1] * 256;
                    return offset;
                }
                offset++;
            }
        }

        return null;
    }
}
