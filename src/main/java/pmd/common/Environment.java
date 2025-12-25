
package pmd.common;

import java.util.ArrayList;
import java.util.List;


public class Environment {

    private final List<String> envs;

    public Environment() {
        envs = new ArrayList<>();
    }

    public void addEnv(String envName) {
        var env = System.getenv(envName);
        if (env != null && !env.isEmpty()) {
            envs.add("%s=%s".formatted(envName, env));
        }
    }

    public String[] getEnv() {
        return envs.toArray(String[]::new);
    }

    public String[] getEnvVal(String envName) {
        if (envs == null) return null;

        for (String item : envs) {
            String[] kv = item.split("=");
            if (kv.length != 2) continue;
            if (!kv[0].equalsIgnoreCase(envName)) continue;

            String[] vals = kv[1].split(";");
            return vals;
        }

        return null;
    }
}
