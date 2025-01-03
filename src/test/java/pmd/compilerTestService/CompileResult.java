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

    public CompileStatus Status;

    public CompileResult.CompileStatus getStatus() {
        return Status;
    }

    private int ExitCode;

    public int getExitCode() {
        return ExitCode;
    }

    private byte[] CompiledBinary;

    public byte[] getCompiledBinary() {
        return CompiledBinary;
    }

    private String Log;

    public String getLog() {
        return Log;
    }

    private Integer MemoWriteAddress;

    public int getMemoWriteAddress() {
        return MemoWriteAddress;
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
                Status = CompileStatus.Succeeded;
            } else {
                Status = CompileStatus.Warning;
            }
        } else {
            if (!log.contains("Exception")) {
                Status = CompileStatus.Failed;
            } else {
                Status = CompileStatus.Exception;
            }
        }
        ExitCode = exitCode;
        CompiledBinary = compiledBinary;
        Log = log;
        MemoWriteAddress = memoWriteAddress < 0 ? null : memoWriteAddress;
    }

    public void WriteLog(Logger logger) {
        switch (Status) {
            case Failed:
                logger.log(Level.ERROR, Log);
                break;
            case Exception:
                logger.log(Level.ERROR, Log);
                break;
            case Warning:
                logger.log(Level.WARNING, Log);
                break;
        }
    }

    public CompileResult.CompareResult Compare(CompileResult target) {
        if (Status == CompileStatus.Failed || target.Status == CompileStatus.Failed ||
                Status == CompileStatus.Exception || target.Status == CompileStatus.Exception ||
                CompiledBinary == null || target.CompiledBinary == null) {
            return CompareResult.Unspecified;
        }
        int size = Math.min(CompiledBinary.length, target.CompiledBinary.length);

        for (int i = 0; i < size; i++) {
            if (CompiledBinary[i] != target.CompiledBinary[i]) {
                var offset = GetMemoOffset(CompiledBinary) != null ? MemoWriteAddress != null ? target.MemoWriteAddress != null ? size : 0 : 0 : 0;

                return i >= offset ? CompareResult.Match_WithoutMemo : CompareResult.Unmatch;
            }
        }

        return CompiledBinary.length == target.CompiledBinary.length ? CompareResult.Match : CompareResult.Match_NotEqualLength;
    }

    public static Integer GetMemoOffset(byte[] array) {
/*
            //  正攻法 (/v あり時のみ)
            if (array.length >= 0x1a && array[1] == 0x1a)
            {
                var offset = array[0x19] + array[0x1a] * 256 - 4 + 1;
                offset = array[offset] + array[offset + 1] * 256 + 1;
                offset = array[offset] + array[offset + 1] * 256;
                return offset;
            }
*/
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
