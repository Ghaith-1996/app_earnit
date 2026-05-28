package com.restlock.domain

object ExerciseCatalog {
    val exercises: List<ExerciseDefinition> = listOf(
        // Biceps - dumbbells
        exercise("dumbbell_curl", "Curl halteres debout", MuscleGroup.Biceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("curl_alterne", "Curl alterne", MuscleGroup.Biceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("curl_incline_banc", "Curl incline sur banc", MuscleGroup.Biceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("hammer_curl", "Curl marteau", MuscleGroup.Biceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("curl_concentration", "Curl concentration", MuscleGroup.Biceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("curl_spider", "Curl spider", MuscleGroup.Biceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("curl_zottman", "Curl Zottman", MuscleGroup.Biceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("curl_assis", "Curl assis", MuscleGroup.Biceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("curl_prise_inversee", "Curl prise inversee", MuscleGroup.Biceps, "Avec halteres", ExerciseEquipment.Dumbbell),

        // Biceps - barbell
        exercise("barbell_curl", "Curl barre droite", MuscleGroup.Biceps, "Avec barre", ExerciseEquipment.Barbell),
        exercise("curl_barre_ez", "Curl barre EZ", MuscleGroup.Biceps, "Avec barre", ExerciseEquipment.Barbell),
        exercise("curl_barre_prise_serree", "Curl barre prise serree", MuscleGroup.Biceps, "Avec barre", ExerciseEquipment.Barbell),
        exercise("curl_barre_prise_large", "Curl barre prise large", MuscleGroup.Biceps, "Avec barre", ExerciseEquipment.Barbell),
        exercise("preacher_curl", "Curl preacher / pupitre avec barre EZ", MuscleGroup.Biceps, "Avec barre", ExerciseEquipment.Barbell),

        // Biceps - cable and machine
        exercise("cable_curl", "Curl poulie basse", MuscleGroup.Biceps, "Machine / cable", ExerciseEquipment.Cable),
        exercise("curl_cable_une_main", "Curl cable a une main", MuscleGroup.Biceps, "Machine / cable", ExerciseEquipment.Cable),
        exercise("curl_corde_poulie", "Curl corde a la poulie", MuscleGroup.Biceps, "Machine / cable", ExerciseEquipment.Cable),
        exercise("curl_preacher_machine", "Curl preacher machine", MuscleGroup.Biceps, "Machine / cable", ExerciseEquipment.Machine),
        exercise("curl_machine_assis", "Curl machine assis", MuscleGroup.Biceps, "Machine / cable", ExerciseEquipment.Machine),
        exercise("curl_cable_bras_derriere", "Curl cable bras derriere le corps", MuscleGroup.Biceps, "Machine / cable", ExerciseEquipment.Cable),
        exercise("curl_double_biceps_poulie_haute", "Curl double biceps a la poulie haute", MuscleGroup.Biceps, "Machine / cable", ExerciseEquipment.Cable),

        // Triceps - cable
        exercise("triceps_pushdown", "Pushdown barre droite", MuscleGroup.Triceps, "Avec cable", ExerciseEquipment.Cable),
        exercise("pushdown_corde", "Pushdown corde", MuscleGroup.Triceps, "Avec cable", ExerciseEquipment.Cable),
        exercise("pushdown_barre_v", "Pushdown barre V", MuscleGroup.Triceps, "Avec cable", ExerciseEquipment.Cable),
        exercise("extension_triceps_overhead_corde", "Extension triceps au-dessus de la tete avec corde", MuscleGroup.Triceps, "Avec cable", ExerciseEquipment.Cable),
        exercise("extension_triceps_un_bras_poulie", "Extension triceps a un bras a la poulie", MuscleGroup.Triceps, "Avec cable", ExerciseEquipment.Cable),
        exercise("kickback_cable", "Kickback cable", MuscleGroup.Triceps, "Avec cable", ExerciseEquipment.Cable),
        exercise("extension_cable_prise_inversee", "Extension cable prise inversee", MuscleGroup.Triceps, "Avec cable", ExerciseEquipment.Cable),

        // Triceps - dumbbells
        exercise("overhead_triceps_extension", "Extension triceps au-dessus de la tete", MuscleGroup.Triceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("extension_triceps_deux_mains", "Extension triceps a deux mains", MuscleGroup.Triceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("skull_crusher_halteres", "Skull crusher halteres", MuscleGroup.Triceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("kickback_haltere", "Kickback haltere", MuscleGroup.Triceps, "Avec halteres", ExerciseEquipment.Dumbbell),
        exercise("tate_press", "Tate press", MuscleGroup.Triceps, "Avec halteres", ExerciseEquipment.Dumbbell),

        // Triceps - barbell and bodyweight
        exercise("skull_crusher", "Skull crusher barre EZ", MuscleGroup.Triceps, "Barre / poids du corps", ExerciseEquipment.Barbell),
        exercise("close_grip_bench_press", "Close grip bench press", MuscleGroup.Triceps, "Barre / poids du corps", ExerciseEquipment.Barbell, met = 5.0),
        exercise("bench_dip", "Dips", MuscleGroup.Triceps, "Barre / poids du corps", ExerciseEquipment.Bodyweight, met = 4.0),
        exercise("dips_assistes_machine", "Dips assistes machine", MuscleGroup.Triceps, "Barre / poids du corps", ExerciseEquipment.Machine),
        exercise("pompes_prise_serree", "Pompes prise serree", MuscleGroup.Triceps, "Barre / poids du corps", ExerciseEquipment.Bodyweight, met = 3.8),
        exercise("pompes_diamant", "Pompes diamant", MuscleGroup.Triceps, "Barre / poids du corps", ExerciseEquipment.Bodyweight, met = 4.5),

        // Back - vertical pulls
        exercise("lat_pulldown", "Lat pulldown prise large", MuscleGroup.Back, "Tirages verticaux", ExerciseEquipment.Cable),
        exercise("lat_pulldown_prise_serree", "Lat pulldown prise serree", MuscleGroup.Back, "Tirages verticaux", ExerciseEquipment.Cable),
        exercise("lat_pulldown_prise_neutre", "Lat pulldown prise neutre", MuscleGroup.Back, "Tirages verticaux", ExerciseEquipment.Cable),
        exercise("pull_up", "Tractions pronation", MuscleGroup.Back, "Tirages verticaux", ExerciseEquipment.Bodyweight, met = 7.5),
        exercise("chin_up", "Tractions supination / chin-ups", MuscleGroup.Back, "Tirages verticaux", ExerciseEquipment.Bodyweight, met = 7.5),
        exercise("tractions_assistees", "Tractions assistees", MuscleGroup.Back, "Tirages verticaux", ExerciseEquipment.Machine, met = 5.0),
        exercise("straight_arm_pulldown", "Straight-arm pulldown", MuscleGroup.Back, "Tirages verticaux", ExerciseEquipment.Cable),
        exercise("pullover_cable", "Pull-over cable", MuscleGroup.Back, "Tirages verticaux", ExerciseEquipment.Cable),

        // Back - rows
        exercise("seated_cable_row", "Seated cable row", MuscleGroup.Back, "Rowing / tirages horizontaux", ExerciseEquipment.Cable),
        exercise("barbell_row", "Rowing barre", MuscleGroup.Back, "Rowing / tirages horizontaux", ExerciseEquipment.Barbell, met = 5.0),
        exercise("rowing_haltere_un_bras", "Rowing haltere a un bras", MuscleGroup.Back, "Rowing / tirages horizontaux", ExerciseEquipment.Dumbbell),
        exercise("rowing_t_bar", "Rowing T-bar", MuscleGroup.Back, "Rowing / tirages horizontaux", ExerciseEquipment.Barbell, met = 5.0),
        exercise("rowing_machine_back", "Rowing machine", MuscleGroup.Back, "Rowing / tirages horizontaux", ExerciseEquipment.Machine),
        exercise("chest_supported_row", "Chest-supported row", MuscleGroup.Back, "Rowing / tirages horizontaux", ExerciseEquipment.Machine),
        exercise("rowing_smith_machine", "Rowing Smith machine", MuscleGroup.Back, "Rowing / tirages horizontaux", ExerciseEquipment.Machine),
        exercise("rowing_poulie_basse", "Rowing poulie basse", MuscleGroup.Back, "Rowing / tirages horizontaux", ExerciseEquipment.Cable),
        exercise("rowing_prise_neutre", "Rowing prise neutre", MuscleGroup.Back, "Rowing / tirages horizontaux", ExerciseEquipment.Cable),
        exercise("rowing_inverse_poids_du_corps", "Rowing inverse au poids du corps", MuscleGroup.Back, "Rowing / tirages horizontaux", ExerciseEquipment.Bodyweight, met = 4.0),

        // Back - lower back
        exercise("deadlift", "Deadlift", MuscleGroup.Back, "Bas du dos", ExerciseEquipment.Barbell, met = 5.0),
        exercise("back_romanian_deadlift", "Romanian deadlift", MuscleGroup.Back, "Bas du dos", ExerciseEquipment.Barbell, met = 5.0),
        exercise("hyperextensions", "Hyperextensions", MuscleGroup.Back, "Bas du dos", ExerciseEquipment.Bodyweight),
        exercise("good_morning_back", "Good morning", MuscleGroup.Back, "Bas du dos", ExerciseEquipment.Barbell, met = 5.0),
        exercise("back_extension", "Back extension machine", MuscleGroup.Back, "Bas du dos", ExerciseEquipment.Machine),

        // Shoulders - front delt
        exercise("shoulder_press_halteres", "Shoulder press halteres", MuscleGroup.Shoulders, "Avant d'epaule", ExerciseEquipment.Dumbbell, met = 5.0),
        exercise("shoulder_press_machine", "Shoulder press machine", MuscleGroup.Shoulders, "Avant d'epaule", ExerciseEquipment.Machine),
        exercise("overhead_press", "Overhead press", MuscleGroup.Shoulders, "Avant d'epaule", ExerciseEquipment.Barbell, met = 5.0),
        exercise("military_press_barre", "Military press barre", MuscleGroup.Shoulders, "Avant d'epaule", ExerciseEquipment.Barbell, met = 5.0),
        exercise("arnold_press", "Arnold press", MuscleGroup.Shoulders, "Avant d'epaule", ExerciseEquipment.Dumbbell),
        exercise("front_raise", "Front raise halteres", MuscleGroup.Shoulders, "Avant d'epaule", ExerciseEquipment.Dumbbell),
        exercise("front_raise_cable", "Front raise cable", MuscleGroup.Shoulders, "Avant d'epaule", ExerciseEquipment.Cable),
        exercise("front_raise_disque", "Front raise disque", MuscleGroup.Shoulders, "Avant d'epaule", ExerciseEquipment.Other),

        // Shoulders - side delt
        exercise("lateral_raise", "Lateral raise halteres", MuscleGroup.Shoulders, "Cote d'epaule", ExerciseEquipment.Dumbbell),
        exercise("lateral_raise_cable", "Lateral raise cable", MuscleGroup.Shoulders, "Cote d'epaule", ExerciseEquipment.Cable),
        exercise("lateral_raise_machine", "Lateral raise machine", MuscleGroup.Shoulders, "Cote d'epaule", ExerciseEquipment.Machine),
        exercise("leaning_lateral_raise", "Leaning lateral raise", MuscleGroup.Shoulders, "Cote d'epaule", ExerciseEquipment.Dumbbell),
        exercise("upright_row_shoulders", "Upright row", MuscleGroup.Shoulders, "Cote d'epaule", ExerciseEquipment.Barbell),
        exercise("dumbbell_high_pull", "Dumbbell high pull", MuscleGroup.Shoulders, "Cote d'epaule", ExerciseEquipment.Dumbbell, met = 5.0),

        // Shoulders - rear delt
        exercise("reverse_pec_deck", "Reverse pec deck", MuscleGroup.Shoulders, "Arriere d'epaule", ExerciseEquipment.Machine),
        exercise("face_pull", "Face pull", MuscleGroup.Shoulders, "Arriere d'epaule", ExerciseEquipment.Cable),
        exercise("rear_delt_fly", "Rear delt fly halteres", MuscleGroup.Shoulders, "Arriere d'epaule", ExerciseEquipment.Dumbbell),
        exercise("rear_delt_fly_cable", "Rear delt fly cable", MuscleGroup.Shoulders, "Arriere d'epaule", ExerciseEquipment.Cable),
        exercise("bent_over_lateral_raise", "Bent-over lateral raise", MuscleGroup.Shoulders, "Arriere d'epaule", ExerciseEquipment.Dumbbell),
        exercise("rowing_coude_ouvert_arriere_epaule", "Rowing coude ouvert pour arriere d'epaule", MuscleGroup.Shoulders, "Arriere d'epaule", ExerciseEquipment.Cable),

        // Chest - presses
        exercise("bench_press", "Bench press barre", MuscleGroup.Chest, "Developpes", ExerciseEquipment.Barbell),
        exercise("bench_press_halteres", "Bench press halteres", MuscleGroup.Chest, "Developpes", ExerciseEquipment.Dumbbell),
        exercise("incline_bench_press_barre", "Incline bench press barre", MuscleGroup.Chest, "Developpes", ExerciseEquipment.Barbell),
        exercise("incline_dumbbell_press", "Incline dumbbell press", MuscleGroup.Chest, "Developpes", ExerciseEquipment.Dumbbell),
        exercise("decline_bench_press", "Decline bench press", MuscleGroup.Chest, "Developpes", ExerciseEquipment.Barbell),
        exercise("chest_press_machine", "Chest press machine", MuscleGroup.Chest, "Developpes", ExerciseEquipment.Machine),
        exercise("smith_machine_bench_press", "Smith machine bench press", MuscleGroup.Chest, "Developpes", ExerciseEquipment.Machine),
        exercise("push_up", "Push-ups", MuscleGroup.Chest, "Developpes", ExerciseEquipment.Bodyweight, met = 3.8),
        exercise("chest_dip", "Dips penches pour pectoraux", MuscleGroup.Chest, "Developpes", ExerciseEquipment.Bodyweight, met = 5.0),

        // Chest - flys
        exercise("dumbbell_fly", "Dumbbell fly", MuscleGroup.Chest, "Ecartes", ExerciseEquipment.Dumbbell),
        exercise("cable_fly", "Cable fly", MuscleGroup.Chest, "Ecartes", ExerciseEquipment.Cable),
        exercise("pec_deck", "Pec deck", MuscleGroup.Chest, "Ecartes", ExerciseEquipment.Machine),
        exercise("incline_cable_fly", "Incline cable fly", MuscleGroup.Chest, "Ecartes", ExerciseEquipment.Cable),
        exercise("low_to_high_cable_fly", "Low-to-high cable fly", MuscleGroup.Chest, "Ecartes", ExerciseEquipment.Cable),
        exercise("high_to_low_cable_fly", "High-to-low cable fly", MuscleGroup.Chest, "Ecartes", ExerciseEquipment.Cable),
        exercise("machine_fly", "Machine fly", MuscleGroup.Chest, "Ecartes", ExerciseEquipment.Machine),

        // Chest - other
        exercise("pullover_haltere", "Pull-over haltere", MuscleGroup.Chest, "Autres", ExerciseEquipment.Dumbbell),
        exercise("svend_press", "Svend press", MuscleGroup.Chest, "Autres", ExerciseEquipment.Other),
        exercise("landmine_press", "Landmine press", MuscleGroup.Chest, "Autres", ExerciseEquipment.Barbell, met = 5.0),

        // Legs - quadriceps
        exercise("barbell_squat", "Squat barre", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Barbell, met = 5.0),
        exercise("front_squat", "Front squat", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Barbell, met = 5.0),
        exercise("hack_squat", "Hack squat", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Machine, met = 5.0),
        exercise("leg_press", "Leg press", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Machine),
        exercise("leg_extension", "Leg extension", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Machine),
        exercise("bulgarian_split_squat", "Bulgarian split squat", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Dumbbell, met = 5.0),
        exercise("walking_lunge", "Walking lunges", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Dumbbell, met = 3.8),
        exercise("reverse_lunges", "Reverse lunges", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Dumbbell, met = 3.8),
        exercise("step_ups", "Step-ups", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Dumbbell, met = 4.0),
        exercise("goblet_squat", "Goblet squat", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Dumbbell, met = 4.5),
        exercise("smith_machine_squat", "Smith machine squat", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Machine, met = 5.0),
        exercise("sissy_squat", "Sissy squat", MuscleGroup.Legs, "Quadriceps", ExerciseEquipment.Bodyweight, met = 4.0),

        // Legs - hamstrings
        exercise("romanian_deadlift", "Romanian deadlift", MuscleGroup.Legs, "Ischio-jambiers", ExerciseEquipment.Barbell, met = 5.0),
        exercise("leg_curl_couche", "Leg curl couche", MuscleGroup.Legs, "Ischio-jambiers", ExerciseEquipment.Machine),
        exercise("leg_curl_assis", "Leg curl assis", MuscleGroup.Legs, "Ischio-jambiers", ExerciseEquipment.Machine),
        exercise("single_leg_leg_curl", "Single-leg leg curl", MuscleGroup.Legs, "Ischio-jambiers", ExerciseEquipment.Machine),
        exercise("good_morning_legs", "Good morning", MuscleGroup.Legs, "Ischio-jambiers", ExerciseEquipment.Barbell, met = 5.0),
        exercise("nordic_hamstring_curl", "Nordic hamstring curl", MuscleGroup.Legs, "Ischio-jambiers", ExerciseEquipment.Bodyweight, met = 4.5),
        exercise("glute_ham_raise", "Glute-ham raise", MuscleGroup.Legs, "Ischio-jambiers", ExerciseEquipment.Bodyweight, met = 4.5),
        exercise("stiff_leg_deadlift", "Stiff-leg deadlift", MuscleGroup.Legs, "Ischio-jambiers", ExerciseEquipment.Barbell, met = 5.0),

        // Legs - glutes
        exercise("hip_thrust_barre", "Hip thrust barre", MuscleGroup.Legs, "Fessiers / Glutes", ExerciseEquipment.Barbell),
        exercise("hip_thrust_machine", "Hip thrust machine", MuscleGroup.Legs, "Fessiers / Glutes", ExerciseEquipment.Machine),
        exercise("glute_bridge", "Glute bridge", MuscleGroup.Legs, "Fessiers / Glutes", ExerciseEquipment.Bodyweight),
        exercise("cable_kickback", "Cable kickback", MuscleGroup.Legs, "Fessiers / Glutes", ExerciseEquipment.Cable),
        exercise("abductor_machine", "Abductor machine", MuscleGroup.Legs, "Fessiers / Glutes", ExerciseEquipment.Machine),
        exercise("glute_bulgarian_split_squat", "Bulgarian split squat", MuscleGroup.Legs, "Fessiers / Glutes", ExerciseEquipment.Dumbbell, met = 5.0),
        exercise("glute_step_ups", "Step-ups", MuscleGroup.Legs, "Fessiers / Glutes", ExerciseEquipment.Dumbbell, met = 4.0),
        exercise("glute_romanian_deadlift", "Romanian deadlift", MuscleGroup.Legs, "Fessiers / Glutes", ExerciseEquipment.Barbell, met = 5.0),
        exercise("sumo_deadlift", "Sumo deadlift", MuscleGroup.Legs, "Fessiers / Glutes", ExerciseEquipment.Barbell, met = 5.0),
        exercise("frog_pumps", "Frog pumps", MuscleGroup.Legs, "Fessiers / Glutes", ExerciseEquipment.Bodyweight),
        exercise("pull_through_cable", "Pull-through cable", MuscleGroup.Legs, "Fessiers / Glutes", ExerciseEquipment.Cable),

        // Legs - calves
        exercise("calf_raise", "Standing calf raise", MuscleGroup.Legs, "Mollets / Calves", ExerciseEquipment.Machine),
        exercise("seated_calf_raise", "Seated calf raise", MuscleGroup.Legs, "Mollets / Calves", ExerciseEquipment.Machine),
        exercise("calf_raise_leg_press", "Calf raise a la leg press", MuscleGroup.Legs, "Mollets / Calves", ExerciseEquipment.Machine),
        exercise("single_leg_calf_raise", "Single-leg calf raise", MuscleGroup.Legs, "Mollets / Calves", ExerciseEquipment.Bodyweight),
        exercise("donkey_calf_raise", "Donkey calf raise", MuscleGroup.Legs, "Mollets / Calves", ExerciseEquipment.Machine),
        exercise("calf_raise_smith_machine", "Calf raise Smith machine", MuscleGroup.Legs, "Mollets / Calves", ExerciseEquipment.Machine),

        // Core - visible abs
        exercise("crunch", "Crunch", MuscleGroup.Core, "Abdos visibles", ExerciseEquipment.Bodyweight, met = 2.8, minutes = 3),
        exercise("crunch_machine", "Crunch machine", MuscleGroup.Core, "Abdos visibles", ExerciseEquipment.Machine, met = 2.8, minutes = 3),
        exercise("cable_crunch", "Cable crunch", MuscleGroup.Core, "Abdos visibles", ExerciseEquipment.Cable, met = 3.0, minutes = 3),
        exercise("sit_ups", "Sit-ups", MuscleGroup.Core, "Abdos visibles", ExerciseEquipment.Bodyweight, met = 3.0, minutes = 3),
        exercise("reverse_crunch", "Reverse crunch", MuscleGroup.Core, "Abdos visibles", ExerciseEquipment.Bodyweight, met = 2.8, minutes = 3),
        exercise("leg_raises", "Leg raises", MuscleGroup.Core, "Abdos visibles", ExerciseEquipment.Bodyweight, met = 3.0, minutes = 3),
        exercise("hanging_leg_raises", "Hanging leg raises", MuscleGroup.Core, "Abdos visibles", ExerciseEquipment.Bodyweight, met = 3.5, minutes = 3),
        exercise("hanging_knee_raise", "Knee raises", MuscleGroup.Core, "Abdos visibles", ExerciseEquipment.Bodyweight, met = 3.0, minutes = 3),
        exercise("decline_crunch", "Decline crunch", MuscleGroup.Core, "Abdos visibles", ExerciseEquipment.Bodyweight, met = 3.0, minutes = 3),
        exercise("toe_touches", "Toe touches", MuscleGroup.Core, "Abdos visibles", ExerciseEquipment.Bodyweight, met = 2.8, minutes = 3),

        // Core - bracing
        exercise("plank", "Plank", MuscleGroup.Core, "Gainage", ExerciseEquipment.Bodyweight, met = 2.8, minutes = 3),
        exercise("side_plank", "Side plank", MuscleGroup.Core, "Gainage", ExerciseEquipment.Bodyweight, met = 2.8, minutes = 3),
        exercise("hollow_body_hold", "Hollow body hold", MuscleGroup.Core, "Gainage", ExerciseEquipment.Bodyweight, met = 2.8, minutes = 3),
        exercise("dead_bug", "Dead bug", MuscleGroup.Core, "Gainage", ExerciseEquipment.Bodyweight, met = 2.5, minutes = 3),
        exercise("bird_dog", "Bird dog", MuscleGroup.Core, "Gainage", ExerciseEquipment.Bodyweight, met = 2.5, minutes = 3),
        exercise("ab_wheel_rollout", "Ab wheel rollout", MuscleGroup.Core, "Gainage", ExerciseEquipment.Other, met = 4.0, minutes = 3),
        exercise("stability_ball_rollout", "Stability ball rollout", MuscleGroup.Core, "Gainage", ExerciseEquipment.Other, met = 3.5, minutes = 3),

        // Core - obliques
        exercise("russian_twist", "Russian twist", MuscleGroup.Core, "Obliques", ExerciseEquipment.Bodyweight, met = 3.0, minutes = 3),
        exercise("cable_woodchopper", "Cable woodchopper", MuscleGroup.Core, "Obliques", ExerciseEquipment.Cable, met = 3.5, minutes = 3),
        exercise("side_bends_haltere", "Side bends haltere", MuscleGroup.Core, "Obliques", ExerciseEquipment.Dumbbell, met = 3.0, minutes = 3),
        exercise("oblique_crunch", "Oblique crunch", MuscleGroup.Core, "Obliques", ExerciseEquipment.Bodyweight, met = 3.0, minutes = 3),
        exercise("pallof_press", "Pallof press", MuscleGroup.Core, "Obliques", ExerciseEquipment.Cable, met = 3.0, minutes = 3),
        exercise("hanging_windshield_wipers", "Hanging windshield wipers", MuscleGroup.Core, "Obliques", ExerciseEquipment.Bodyweight, met = 4.0, minutes = 3),

        // Traps
        exercise("barbell_shrug", "Barbell shrug", MuscleGroup.Traps, "Trapezes", ExerciseEquipment.Barbell),
        exercise("dumbbell_shrug", "Dumbbell shrug", MuscleGroup.Traps, "Trapezes", ExerciseEquipment.Dumbbell),
        exercise("smith_machine_shrug", "Smith machine shrug", MuscleGroup.Traps, "Trapezes", ExerciseEquipment.Machine),
        exercise("cable_shrug", "Cable shrug", MuscleGroup.Traps, "Trapezes", ExerciseEquipment.Cable),
        exercise("farmers_walk_traps", "Farmer's walk", MuscleGroup.Traps, "Trapezes", ExerciseEquipment.Dumbbell, met = 5.0),
        exercise("upright_row_traps", "Upright row", MuscleGroup.Traps, "Trapezes", ExerciseEquipment.Barbell),
        exercise("rack_pull", "Rack pull", MuscleGroup.Traps, "Trapezes", ExerciseEquipment.Barbell, met = 5.0),
        exercise("face_pull_traps", "Face pull", MuscleGroup.Traps, "Trapezes", ExerciseEquipment.Cable),

        // Forearms
        exercise("wrist_curl", "Wrist curl", MuscleGroup.Forearms, "Avant-bras", ExerciseEquipment.Dumbbell),
        exercise("reverse_wrist_curl", "Reverse wrist curl", MuscleGroup.Forearms, "Avant-bras", ExerciseEquipment.Dumbbell),
        exercise("farmers_walk_forearms", "Farmer's walk", MuscleGroup.Forearms, "Avant-bras", ExerciseEquipment.Dumbbell, met = 5.0),
        exercise("dead_hang", "Dead hang", MuscleGroup.Forearms, "Avant-bras", ExerciseEquipment.Bodyweight),
        exercise("plate_pinch", "Plate pinch", MuscleGroup.Forearms, "Avant-bras", ExerciseEquipment.Other),
        exercise("reverse_curl", "Reverse curl", MuscleGroup.Forearms, "Avant-bras", ExerciseEquipment.Barbell),
        exercise("hammer_curl_forearms", "Hammer curl", MuscleGroup.Forearms, "Avant-bras", ExerciseEquipment.Dumbbell),
        exercise("wrist_roller", "Wrist roller", MuscleGroup.Forearms, "Avant-bras", ExerciseEquipment.Other),
        exercise("grip_trainer", "Grip trainer", MuscleGroup.Forearms, "Avant-bras", ExerciseEquipment.Other),
        exercise("towel_pull_ups", "Towel pull-ups", MuscleGroup.Forearms, "Avant-bras", ExerciseEquipment.Bodyweight, met = 7.5),

        // Neck
        exercise("neck_flexion", "Neck flexion", MuscleGroup.Neck, "Cou", ExerciseEquipment.Other, met = 2.5, minutes = 3),
        exercise("neck_extension", "Neck extension", MuscleGroup.Neck, "Cou", ExerciseEquipment.Other, met = 2.5, minutes = 3),
        exercise("neck_lateral_flexion", "Neck lateral flexion", MuscleGroup.Neck, "Cou", ExerciseEquipment.Other, met = 2.5, minutes = 3),
        exercise("neck_harness", "Neck harness", MuscleGroup.Neck, "Cou", ExerciseEquipment.Other, met = 2.8, minutes = 3),
        exercise("shrugs_lourds", "Shrugs lourds", MuscleGroup.Neck, "Cou", ExerciseEquipment.Barbell),

        // Full body
        exercise("full_body_deadlift", "Deadlift", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Barbell, met = 5.0),
        exercise("full_body_squat", "Squat", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Barbell, met = 5.0),
        exercise("full_body_bench_press", "Bench press", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Barbell),
        exercise("full_body_overhead_press", "Overhead press", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Barbell, met = 5.0),
        exercise("full_body_pull_ups", "Pull-ups", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Bodyweight, met = 7.5),
        exercise("full_body_dips", "Dips", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Bodyweight, met = 5.0),
        exercise("clean", "Clean", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Barbell, met = 6.0),
        exercise("power_clean", "Power clean", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Barbell, met = 6.0),
        exercise("snatch", "Snatch", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Barbell, met = 6.0),
        exercise("thruster", "Thruster", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Barbell, met = 6.0),
        exercise("farmers_walk_full_body", "Farmer's walk", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Dumbbell, met = 5.0),
        exercise("kettlebell_swing", "Kettlebell swing", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Kettlebell, met = 8.0, minutes = 5),
        exercise("mountain_climber", "Mountain climber", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Bodyweight, met = 11.0, minutes = 3),
        exercise("burpees", "Burpees", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Bodyweight, met = 8.0, minutes = 5),
        exercise("sled_push", "Sled push", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Sled, met = 7.0, minutes = 5),
        exercise("sled_pull", "Sled pull", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Sled, met = 7.0, minutes = 5),
        exercise("battle_ropes", "Battle ropes", MuscleGroup.FullBody, "Exercices composes", ExerciseEquipment.Conditioning, met = 9.0, minutes = 5),

        // Existing cardio choices stay available for older users and simple workouts.
        exercise("treadmill_run", "Treadmill run", MuscleGroup.Cardio, "Cardio", ExerciseEquipment.Conditioning, met = 9.8, minutes = 10),
        exercise("stationary_bike", "Stationary bike", MuscleGroup.Cardio, "Cardio", ExerciseEquipment.Machine, met = 6.8, minutes = 10),
        exercise("rowing_machine", "Rowing machine", MuscleGroup.Cardio, "Cardio", ExerciseEquipment.Machine, met = 5.0, minutes = 10),
        exercise("jump_rope", "Jump rope", MuscleGroup.Cardio, "Cardio", ExerciseEquipment.Conditioning, met = 11.0, minutes = 8),
        exercise("stair_climber", "Stair climber", MuscleGroup.Cardio, "Cardio", ExerciseEquipment.Machine, met = 9.3, minutes = 8),
    )

    val groups: List<MuscleGroup> = MuscleGroup.entries
        .filter { group -> exercises.any { it.muscleGroup == group } }

    private val byId: Map<String, ExerciseDefinition> = exercises.associateBy { it.id }

    fun byId(id: String): ExerciseDefinition? = byId[id]

    fun byGroup(group: MuscleGroup): List<ExerciseDefinition> =
        exercises.filter { it.muscleGroup == group }

    private fun exercise(
        id: String,
        name: String,
        group: MuscleGroup,
        section: String,
        equipment: ExerciseEquipment,
        met: Double = 3.5,
        minutes: Int = 4,
    ): ExerciseDefinition {
        return ExerciseDefinition(
            id = id,
            name = name,
            muscleGroup = group,
            section = section,
            equipment = equipment,
            met = met,
            defaultMinutes = minutes,
            metCategory = metCategoryFor(equipment = equipment, met = met),
        )
    }

    private fun metCategoryFor(equipment: ExerciseEquipment, met: Double): String {
        return when {
            met >= 7.0 -> "High-intensity conditioning or vigorous bodyweight work"
            equipment == ExerciseEquipment.Bodyweight -> "Bodyweight resistance training"
            equipment == ExerciseEquipment.Cable -> "Cable resistance training"
            equipment == ExerciseEquipment.Machine -> "Machine resistance training"
            equipment == ExerciseEquipment.Barbell -> "Barbell resistance training"
            equipment == ExerciseEquipment.Dumbbell -> "Dumbbell resistance training"
            else -> "Resistance training"
        }
    }
}
