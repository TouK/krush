# 1.2.0
Infrastructure
* Kotlin [1.8.10](https://github.com/JetBrains/kotlin/releases/tag/v1.8.10)
* Exposed [0.41.1](https://github.com/JetBrains/Exposed/releases/tag/0.41.1)
* axion-release-plugin [1.15.0](https://github.com/allegro/axion-release-plugin/releases/tag/v1.15.0)

Features
* More column wrappers ([161](https://github.com/TouK/krush/pull/161) by [@alien11689](https://github.com/alien11689))

# 1.1.0
Infrastructure
* Kotlin [1.8.0](https://github.com/JetBrains/kotlin/releases/tag/v1.8.0)
* Flyway [9.11.0](https://github.com/flyway/flyway/releases/tag/flyway-9.11.0)

# 1.0.0
🎉🎉🎉 We finally reached 1.0 release! 🎉🎉🎉

Infrastructure
* Kotlin [1.7.10](https://github.com/JetBrains/kotlin/releases/tag/v1.7.10)
* Exposed [0.38.2](https://github.com/JetBrains/Exposed/releases/tag/0.38.2)
* kotlin-poet [1.12.0](https://github.com/square/kotlinpoet/releases/tag/1.12.0)

Features
* Switched to build.gradle.kts ([112](https://github.com/TouK/krush/pull/122))
* First initial refactor of SelfReferencesFunctions.kt class ([85](https://github.com/TouK/krush/pull/85))

Bug fixes
* Kapt task fails if @kotlinx.serialization.Serializable is also applied to the class ([96](https://github.com/TouK/krush/issues/96))

# 0.6.0
Infrastructure
* Kotlin [1.6.10](https://github.com/JetBrains/kotlin/releases/tag/v1.6.10)
* Exposed [0.37.2](https://github.com/JetBrains/Exposed/releases/tag/0.37.2)
* kotlin-poet [1.10.2](https://github.com/square/kotlinpoet/releases/tag/1.10.2)

Bug fixes
* Breaking tests due to old exposed version ([60](https://github.com/TouK/krush/issues/60))
* Self-referenced entities generate invalid code ([22](https://github.com/TouK/krush/issues/22))
* Fix indentation in generated code ([51](https://github.com/TouK/krush/issues/51))

# 0.5.1
Bug fixes
* @JoinColumn name ignored when using @ManyToOne ([53](https://github.com/TouK/krush/issues/53))

# 0.5.0
Infrastructure
* Kotlin [1.5.10](https://github.com/JetBrains/kotlin/releases/tag/v1.5.10)
* Exposed [0.32.1](https://github.com/JetBrains/Exposed/releases/tag/0.32.1)
* kotlin-poet [1.9.0](https://github.com/square/kotlinpoet/releases/tag/1.9.0)

Features
* Conversion of TypeMirror and TypeElement is deprecated in KotlinPoet, use kotlin-metadata APIs instead ([49](https://github.com/TouK/krush/issues/49))

# 0.4.0
Infrastructure
* Kotlin [1.4.31](https://github.com/JetBrains/kotlin/releases/tag/v1.4.31)
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
