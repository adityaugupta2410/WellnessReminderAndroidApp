package com.example.ui.model

data class StretchItem(
    val title: String,
    val description: String,
    val difficulty: String,
    val steps: List<String>,
    val durationSeconds: Int,
    val targetArea: String
)

data class FactItem(
    val category: String, // "WATER", "WALK", "STRETCH", "MINDFUL", "GENERAL"
    val fact: String,
    val sourceOrStat: String
)

data class MindfulExercise(
    val title: String,
    val description: String,
    val durationSeconds: Int,
    val breathingCadence: String,
    val steps: List<String>
)

object WellnessData {
    val mindfulExercises = listOf(
        MindfulExercise(
            title = "Box Breathing Reset",
            description = "A powerful, standard technique used by high-performance professionals to instantly clear mental clutter and stabilize heart rate variability.",
            durationSeconds = 60,
            breathingCadence = "Inhale 4s · Hold 4s · Exhale 4s · Hold 4s",
            steps = listOf(
                "Sit comfortably, release your shoulders, and exhale all air fully.",
                "Inhale slowly through your nose for 4 seconds, filling your abdomen.",
                "Hold your breath gently for 4 solid seconds.",
                "Exhale thoroughly through your mouth for 4 seconds.",
                "Hold empty for 4 seconds before starting the next breath loop."
            )
        ),
        MindfulExercise(
            title = "Vagus Nerve Stabilizer",
            description = "Lengthens the exhale period to stimulate the body's parasympathetic rest-and-digest response, turning off acute desk stress.",
            durationSeconds = 60,
            breathingCadence = "Inhale 4s · Exhale 8s (Long & Slow)",
            steps = listOf(
                "Sit erect, feet flat. Place one hand gently on your belly.",
                "Breathe in through your nose for 4 seconds, sensing your hand rise.",
                "Pucker your lips slightly as if breathing through a straw.",
                "Exhale very slowly and steadily for a full, relaxing 8 seconds."
            )
        ),
        MindfulExercise(
            title = "Digital Eye Strain Decompress",
            description = "Integrates smooth palming and mindfulness to recharge optic nerves tired from blue-light emitting desktop screens.",
            durationSeconds = 45,
            breathingCadence = "Steady Micro-Inhales & Exhales",
            steps = listOf(
                "Rub your hands together vigorously for 10 seconds until they feel warm.",
                "Close your eyes and gently cupping your warm palms over them.",
                "Sense the darkness soothing your eye muscles and optic fibers.",
                "Take 3 long, calm breaths, releasing all facial or forehead strain."
            )
        )
    )

    val stretches = listOf(
        StretchItem(
            title = "Neck Strain Release",
            description = "Alleviates neck and upper neck cervical stiffness caused by staring forward at monitors.",
            difficulty = "Easy & Safe",
            steps = listOf(
                "Sit upright and let your shoulders drop down naturally.",
                "Slowly tilt your right ear toward your right shoulder until you feel a gentle stretch.",
                "Hold for 10-15 seconds while breathing deeply.",
                "Slowly return to center and repeat on the left side."
            ),
            durationSeconds = 30,
            targetArea = "Neck & Upper Shoulders"
        ),
        StretchItem(
            title = "Desk Shoulder Rolls",
            description = "Resets rounded shoulders and opens up chests to counteract slouching or posture fatigue.",
            difficulty = "Easy & Seated",
            steps = listOf(
                "Sit straight with feet flat on the floor.",
                "Roll your shoulders upward toward your ears, then rotate them backward in a smooth circle.",
                "Draw your shoulder blades down and forward to complete the circle.",
                "Repeat backward 5 times, then forward 5 times."
            ),
            durationSeconds = 20,
            targetArea = "Upper Back & Chest"
        ),
        StretchItem(
            title = "Wrist Flexor Stretch",
            description = "Eases tension in your forearms and wrists developed from intense typing and mouse grip.",
            difficulty = "Easy & Low Strain",
            steps = listOf(
                "Extend your right arm straight in front of you, palm facing away and fingers down.",
                "Use your left hand to gently guide the fingertips back toward your body.",
                "Hold the gentle stretch for 10-15 seconds. Do not over-extend.",
                "Switch hands and repeat for your left forearm."
            ),
            durationSeconds = 30,
            targetArea = "Wrists, Hands & Forearms"
        ),
        StretchItem(
            title = "Upper Back Spinal Twist",
            description = "Re-aligns the lower and mid spine to ease tension from prolonged seated posture.",
            difficulty = "Mild",
            steps = listOf(
                "Sit upright near the front edge of your chair.",
                "Place your left hand on the outside of your right knee.",
                "Place your right arm over the back of the chair for support.",
                "Gently twist your torso to the right, looking back past your shoulder. Hold for 10s.",
                "Release back to center and repeat on the left side."
            ),
            durationSeconds = 25,
            targetArea = "Lower Back & Core Spine"
        ),
        StretchItem(
            title = "Chest Opener Expansion",
            description = "Counteracts the closed posture of hunched desk work, expanding lung capacity and shoulder rotation.",
            difficulty = "Safe & Standing",
            steps = listOf(
                "Interlace your fingers behind your lower back.",
                "Keep your arms straight, and gently pull your shoulders back and chest outwards.",
                "Slightly lift your chin and take 3 deep, mindful abdominal breaths.",
                "Release gently and roll your arms out."
            ),
            durationSeconds = 20,
            targetArea = "Chest & Collarbone"
        )
    )

    val facts = listOf(
        FactItem(
            category = "WATER",
            fact = "Being just 2% dehydrated can cause an 11% drop in brain computational speed and decision making.",
            sourceOrStat = "Brain Research Journal"
        ),
        FactItem(
            category = "WATER",
            fact = "Drinking water before or during tasks reduces brain fatigue, acts as a solvent for nutrients, and accelerates oxygen transport.",
            sourceOrStat = "Harvard Medicine"
        ),
        FactItem(
            category = "WATER",
            fact = "Staying hydrated throughout the afternoon keeps your metabolism high and prevents the classic post-lunch slump.",
            sourceOrStat = "Clinical Nutrition Review"
        ),
        FactItem(
            category = "WALK",
            fact = "Taking a 2-minute active walking break every hour reduces sitting-related glucose spikes by up to 30%.",
            sourceOrStat = "Stanford Medicine"
        ),
        FactItem(
            category = "WALK",
            fact = "Standing or walking for even 90 seconds triggers lower body muscle activity, shifting blood pooling away from your lower extremities.",
            sourceOrStat = "Cardiology Association"
        ),
        FactItem(
            category = "WALK",
            fact = "Brief movement breaks stimulate the hippocampus, boosting verbal memory, speech planning, and mental focus.",
            sourceOrStat = "Harvard Health"
        ),
        FactItem(
            category = "STRETCH",
            fact = "Periodic stretching releases tight lactic acid from desktop muscles, promoting uniform oxygen distribution to cells.",
            sourceOrStat = "Mayo Clinic Research"
        ),
        FactItem(
            category = "STRETCH",
            fact = "Gentle forearm and wrist stretches lower muscle stress in the carpal region by 40%, preventing repetitive strain injuries.",
            sourceOrStat = "Occupational Ergonomics"
        ),
        FactItem(
            category = "MINDFUL",
            fact = "Just 60 seconds of box breathing activates your parasympathetic nervous system, lowering acute cortisol release by 25%.",
            sourceOrStat = "Stanford Neurobiology"
        ),
        FactItem(
            category = "MINDFUL",
            fact = "Longer breath out-phases stimulate the vagus nerve directly, reducing stress-mediated workplace muscle tension instantly.",
            sourceOrStat = "Psychiatric Research Today"
        ),
        FactItem(
            category = "MINDFUL",
            fact = "Taking regular breathing micro-breaks boosts cognitive flexibility and high-pressure decision accuracy by 18%.",
            sourceOrStat = "Journal of Applied Psychology"
        ),
        FactItem(
            category = "GENERAL",
            fact = "Desk professionals who take smart micro-breaks report an 85% higher level of daily job satisfaction and low systemic fatigue.",
            sourceOrStat = "Global Health Analytics"
        )
    )

    fun getFactForCategory(category: String): FactItem {
        val filtered = facts.filter { it.category == category || it.category == "GENERAL" }
        return filtered.randomOrNull() ?: FactItem("GENERAL", "Moving and breathing deeply at work resets physical stress and improves mood.", "Clinical Studies")
    }

    fun getStretchForAlert(): StretchItem {
        return stretches.random()
    }

    fun getMindfulExerciseForAlert(): MindfulExercise {
        return mindfulExercises.random()
    }
}
