public enum ValueType {
    INT,
    DOUBLE,
    BOOLEAN,  // result of a comparison / equality expression
    ERROR     // type already invalid; used to avoid cascading error messages
}
