package io.kixi

enum class Type(supertype: Type?) {
    // Super types
    Any(null),
    Number(Any),

    // Base types
    String(Any), Char(Any),
    Int(Number), Long(Number),
    Float(Number), Double(Number), Decimal(Number),
    Bool(Any),
    URL(Any),
    Date(Any), LocalDateTime(Any), ZonedDateTime(Any),
    Duration(Any),
    Version(Any),
    Blob(Any),
    Quantity(Any),
    Range(Any),
    List(Any), Map(Any),

    // nil
    nil(null);
}