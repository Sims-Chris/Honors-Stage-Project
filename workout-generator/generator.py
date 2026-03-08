import pandas as pd

EXERCISE_LIBRARY = {
    "pull_strength": [
        "Weighted Pull-ups", "French Pull-ups", "Lock-off intervals", 
        "One-arm scapular pulls", "Typewriter pull-ups", "Weighted chin-ups"
    ],
    "push_strength": [
        "Ring Dips", "Weighted Push-ups", "Overhead Press", 
        "Pike push-ups", "Pseudo planche push-ups", "Tricep extensions"
    ],
    "finger_strength": [
        "Max Hangs on 20mm edge", "Half-crimp strict hangs", "Minimum edge hangs", 
        "One-arm hangs (assisted)", "Drag grip hangs", "Pinch block lifts"
    ],
    "finger_endurance": [
        "Repeaters", "4x4 Bouldering intervals", "ARC traversing", 
        "Foot-on campus ladders", "Treadwall laps", "Density hangs"
    ],
    "power": [
        "Explosive Campus Board reach", "Limit Bouldering", "Plyometric Pull-ups", 
        "Double dyno practice", "Campus board bumps", "Moonboard session"
    ],
    "flexibility": [
        "Pigeon pose", 
        "Frog stretch", 
        "Seated pancake stretch", 
        "Banded shoulder dislocates", 
        "Puppy pose (lat stretch)", 
        "Kneeling forearm stretches"
    ]
}
def load_data(filepath):
    """Loads the benchmark climbing data."""
    try:
        df = pd.read_csv(filepath)
        return df
    except FileNotFoundError:
        print(f"Error: Could not find {filepath}. Make sure it is in the same folder.")
        return None

def analyze_weaknesses(user_inputs, df):
    """Compares user stats to the target grade to find weaknesses."""
    current_grade = user_inputs["Grade"]
    target_grade = current_grade + 1
    
    # Check if target grade exists in data; if not, use the highest available
    if target_grade not in df['bouldergrade_numeric'].values:
        target_grade = df['bouldergrade_numeric'].max()
        
    target_stats = df[df['bouldergrade_numeric'] == target_grade].iloc[0]
    weaknesses = []
    
    # Compare user stats to the median target stats
    if user_inputs["Pull ups"] < target_stats.get("pullup reps_median", 15):
        weaknesses.append("pull_strength")
    if user_inputs["Push ups"] < target_stats.get("pushup reps_median", 25):
        weaknesses.append("push_strength")
    if user_inputs["Max hang"] < target_stats.get("maxhang_median", 50.0):
        weaknesses.append("finger_strength")
    if user_inputs["Continuous hang"] < target_stats.get("continuoushang_median", 40.0):
        weaknesses.append("finger_endurance")
    if user_inputs["Explosive campus"] < target_stats.get("bigmovecamp_median", 70.0):
        weaknesses.append("power")
        
    return weaknesses if weaknesses else ["finger_strength", "pull_strength", "power"]

def generate_dynamic_prescription(exercise_name, category, current_week, grade):
    """
    Dynamically generates reps and sets based on the exercise type, 
    the climber's grade, and progressive overload (current week).
    """
    # Progressive overload: increase volume slightly every 2 weeks
    base_sets = 3 + (current_week // 2) 
    
    if "strength" in category or "power" in category:
        # Strength/Power relies on low reps, high intensity
        reps = f"{3 + (current_week % 3)} reps" 
    elif "endurance" in category:
        # Endurance relies on higher volume/time under tension
        reps = f"{10 + (current_week * 2)} reps or 1 min continuous"
    elif "flexibility" in category:
        reps = "30s hold per side"
        base_sets = 2
    else:
        reps = "8 reps"
        
    # Edge cases for time-based exercises (hangboards)
    if "hang" in exercise_name.lower() or "repeaters" in exercise_name.lower():
        reps = f"{7}s on / 3s off (x{4 + current_week} cycles)"
        
    return {
        "name": exercise_name,
        "reps": reps,
        "sets": str(base_sets)
    }

def generate_plan(weaknesses, user_inputs):
    """Creates a weekly schedule utilizing specific training days and dynamic volume."""
    training_days = user_inputs["Training Days"]
    total_weeks = user_inputs["Length of time (weeks)"]
    flexibility_score = user_inputs["Flexibility (0-10)"]
    
    print(f"\n--- Generating {total_weeks}-Week Training Plan ---")
    print(f"Focus areas identified: {', '.join(weaknesses).replace('_', ' ').title()}\n")
    
    for week in range(1, total_weeks + 1):
        print(f"================ WEEK {week} ================")
        
        for index, day in enumerate(training_days):
            print(f"\n### {day} ###")
            print("- Warmup: 15 mins easy bouldering")
            
            # Rotate through weaknesses based on the day index
            focus_1 = weaknesses[(index) % len(weaknesses)]
            focus_2 = weaknesses[(index + 1) % len(weaknesses)]
            
            # Fetch base exercise name from library
            ex1_name = EXERCISE_LIBRARY[focus_1][(week - 1) % len(EXERCISE_LIBRARY[focus_1])]
            ex2_name = EXERCISE_LIBRARY[focus_2][(week) % len(EXERCISE_LIBRARY[focus_2])]
            
            # Dynamically generate reps and sets
            primary_task = generate_dynamic_prescription(ex1_name, focus_1, week, user_inputs["Grade"])
            secondary_task = generate_dynamic_prescription(ex2_name, focus_2, week, user_inputs["Grade"])
            
            print(f"Primary Focus ({focus_1.title()}): \n  {primary_task}")
            print(f"Secondary Focus ({focus_2.title()}): \n  {secondary_task}")
            
            # Cooldown logic
            if flexibility_score > 5:
                flex_ex = EXERCISE_LIBRARY["flexibility"][(index) % len(EXERCISE_LIBRARY["flexibility"])]
                cooldown_task = generate_dynamic_prescription(flex_ex, "flexibility", week, user_inputs["Grade"])
                print(f"Cooldown: \n  {cooldown_task}")
            elif flexibility_score > 0:
                print("Cooldown: \n  {'name': 'Basic stretching', 'reps': '20s holds', 'sets': '1'}")
        print("\n")

# --- Main Execution ---
if __name__ == "__main__":
    file_name = "workout-generator\\Data\\_Final_boulder_grade_stats_flat.csv"
    benchmark_data = load_data(file_name)
    
    # Simulate User Inputs (Notice the change to 'Training Days')
    user_profile = {
        "Grade": 5.0, # e.g., V5
        "Pull ups": 8,
        "Push ups": 15,
        "Continuous hang": 30.0,
        "Max hang": 40.0,
        "1 Rep max weighted pull up": 50.0,
        "Repeaters": 80.0,
        "Explosive campus": 65.0,
        "Flexibility (0-10)": 8,
        "Length of time (weeks)": 4,
        "Training Days": ["Monday", "Wednesday", "Friday"] 
    }
    
    # In a real scenario, skip generating if benchmark_data is None. 
    # For proof of concept without the CSV, we simulate a dummy df:
    if benchmark_data is None:
        print("CSV not found! Simulating with dummy data for POC demonstration...\n")
        benchmark_data = pd.DataFrame({'bouldergrade_numeric': [6.0]})
        
    user_weaknesses = analyze_weaknesses(user_profile, benchmark_data)
    generate_plan(user_weaknesses, user_profile)