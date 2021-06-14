# 0.4.0
Infrastructure
* Kotlin 1.4.31
* Exposed [0.31.1](https://github.com/JetBrains/Exposed/releases/tag/0.31.1)
* kotlin-poet 1.7.2

Features
* Support converter as companion object ([33](https://github.com/TouK/krush/issues/33))
* Support @EmbeddedId ([34](https://github.com/TouK/krush/issues/34]))
* Support @Id on same column as @JoinColumn or @PrimaryKeyJoinColumn ([35](https://github.com/TouK/krush/issues/35))
* Support shared reference on composite key ([40](https://github.com/TouK/krush/issues/40))
* PostgreSQL JSONB support ([43](https://github.com/TouK/krush/issues/43))


Bug fixes
* Typo in PropertyTypeNotSupportedException ([28](https://github.com/TouK/krush/issues/27))
* Nullable @Embedded causing compile error ([29](https://github.com/TouK/krush/issues/29))
* Nullable field in @Embeddable causing compile error ([31](https://github.com/TouK/krush/issues/31))


# 0.3.0
Infrastructure
* Exposed 0.24.1

# 0.2.0
Infrastructure
* Exposed 0.18.1
