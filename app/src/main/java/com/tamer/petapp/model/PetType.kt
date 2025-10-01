package com.tamer.petapp.model

enum class PetType {
    DOG,
    CAT,
    BIRD,
    FISH,
    RABBIT,
    HAMSTER,
    OTHER;

    companion object {
        fun fromString(type: String): PetType {
            return when (type) {
                "Köpek" -> DOG
                "Kedi" -> CAT
                "Kuş" -> BIRD
                "Balık" -> FISH
                "Tavşan" -> RABBIT
                "Hamster" -> HAMSTER
                else -> OTHER
            }
        }

        fun toString(type: PetType): String {
            return when (type) {
                DOG -> "Köpek"
                CAT -> "Kedi"
                BIRD -> "Kuş"
                FISH -> "Balık"
                RABBIT -> "Tavşan"
                HAMSTER -> "Hamster"
                OTHER -> "Diğer"
            }
        }
    }
} 