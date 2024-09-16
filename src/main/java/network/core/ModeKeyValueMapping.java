package network.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This class is used to store the key-value mapping for different modes.
 */

public final class ModeKeyValueMapping {

    private final TransMode.Mode mode;
    private final Set<Map<String, String>> keyValueMapping;

    private ModeKeyValueMapping(Builder builder){
        this.mode = builder.mode;
        this.keyValueMapping = builder.keyValueMapping;
    }

    public TransMode.Mode getMode(){
        return mode;
    }

    public Set<Map<String, String>> getKeyValueMapping(){
        return keyValueMapping;
    }

    public static class Builder{
        private TransMode.Mode mode;
        private Set<Map<String, String>> keyValueMapping = new HashSet<>();

        public Builder setMode(TransMode.Mode mode){
            this.mode = mode;
            return this;
        }

        public Builder setKeyValueMapping(Set<Map<String, String>> keyValueMapping){
            this.keyValueMapping = keyValueMapping;
            return this;
        }

        public Builder addKeyValueMapping(Map<String, String> keyValuePairs){
            this.keyValueMapping.add(keyValuePairs);
            return this;
        }

        public ModeKeyValueMapping build(){
            return new ModeKeyValueMapping(this);
        }
    }
}
