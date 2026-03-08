import pandas as pd
from sklearn.ensemble import RandomForestRegressor
import random
import math
import warnings
warnings.filterwarnings('ignore')

# --- 1. Schema-Aligned Exercise Bank ---
# Format: Name : weight : sets : reps : time for exercise : rest time
exercise_bank = {
    "pulling_strength": [
        {"name": "Weighted pull ups", "weight": "60% of max weight", "sets": "4", "reps": "3", "time": "", "rest": "120"},
        {"name": "Assisted one arm pull ups", "weight": "add or remove weight as needed", "sets": "3", "reps": "2", "time": "", "rest": "120"},
        {"name": "One arm lock offs", "weight": "add or remove weight as needed", "sets": "4", "reps": "1", "time": "8", "rest": "30"}
    ],
    "pulling_endurance": [
        {"name": "Pull ups", "weight": "", "sets": "8", "reps": "DYNAMIC_PULLUP_REP", "time": "", "rest": "30"},
        {"name": "French Medies", "weight": "", "sets": "3", "reps": "1", "time": "Max", "rest": "60"}
    ],
    "finger_strength": [
        {"name": "Pinch block", "weight": "80% of max weight", "sets": "2", "reps": "4", "time": "", "rest": "20"},
        {"name": "Half crimp lift", "weight": "80% of max weight", "sets": "3", "reps": "4", "time": "", "rest": "20"},
        {"name": "Max hangs (20 mm)", "weight": "100% of max weight", "sets": "4", "reps": "1", "time": "8", "rest": "120"},
        {"name": "One arm hangs (20mm)", "weight": "add or remove weight as needed", "sets": "2", "reps": "1", "time": "8", "rest": "30"},
        {"name": "Small edge hangs (12mm)", "weight": "", "sets": "3", "reps": "1", "time": "8", "rest": "60"}
    ],
    "finger_endurance": [
        {"name": "Repeaters", "weight": "", "sets": "2", "reps": "10", "time": "7", "rest": "3"},
        {"name": "Continuous hang", "weight": "bodyweight", "sets": "3", "reps": "1", "time": "Max", "rest": "120"}
    ],
    "explosive_power": [
        {"name": "Campus latches", "weight": "", "sets": "4", "reps": "3", "time": "", "rest": "60"},
        {"name": "Campus double dynos", "weight": "", "sets": "4", "reps": "3", "time": "", "rest": "90"}
    ],
    "antagonist": [
        {"name": "Push ups", "weight": "", "sets": "3", "reps": "DYNAMIC_PUSHUP_REP", "time": "", "rest": "60"},
        {"name": "Overhead Press", "weight": "70% of max weight", "sets": "3", "reps": "8", "time": "", "rest": "90"}
    ]
}

# --- 2. Train AI ---
# Load data (Ensure Bouldering_AI_Ready.csv is in the same folder)
df = pd.read_csv("reducedAssesmentData.csv")
X = df[['bouldergrade_numeric', 'sex_encoded', 'height', 'weight']]
y_cols = ['pullup reps', 'weightedpull', 'maxhang', 'continuoushang', 'repeaters1', 'powl', 'powr', 'pushup reps']
y = df[y_cols].fillna(0)

ai_model = RandomForestRegressor(random_state=42)
ai_model.fit(X, y)

# --- 3. Weekly Planner Logic ---
def generate_schema_plan(target_grade, sex, height, weight, actuals, active_days):
    user_profile = [[target_grade, sex, height, weight]]
    preds = ai_model.predict(user_profile)[0]
    expected = dict(zip(y_cols, preds))
    
    prescriptions = []
    
    # Needs Analysis
    pull_rep_ratio = actuals['pullups'] / expected['pullup reps'] if expected['pullup reps'] else 1
    pull_weight_ratio = actuals['weighted_pull'] / expected['weightedpull'] if expected['weightedpull'] else 1
    if pull_weight_ratio > 0.9 and pull_rep_ratio < 0.8: prescriptions.append("pulling_endurance")
    elif pull_weight_ratio < 0.8 and pull_rep_ratio > 0.9: prescriptions.append("pulling_strength")
    elif pull_weight_ratio < 0.8 and pull_rep_ratio < 0.8: prescriptions.append("pulling_strength")

    hang_ratio = actuals['maxhang'] / expected['maxhang'] if expected['maxhang'] else 1
    endurance_ratio = actuals['continuoushang'] / expected['continuoushang'] if expected['continuoushang'] else 1
    if hang_ratio > 0.9 and endurance_ratio < 0.8: prescriptions.append("finger_endurance")
    elif hang_ratio < 0.8 and endurance_ratio > 0.9: prescriptions.append("finger_strength")
    elif hang_ratio < 0.8 and endurance_ratio < 0.8: prescriptions.append("finger_strength")
        
    actual_power = (actuals['powl'] + actuals['powr']) / 2
    expected_power = (expected['powl'] + expected['powr']) / 2
    if actual_power < (expected_power * 0.85): prescriptions.append("explosive_power")
            
    if actuals['pushups'] < (expected['pushup reps'] * 0.8): prescriptions.append("antagonist")
        
    if not prescriptions:
        prescriptions = ["finger_strength", "pulling_strength", "explosive_power", "antagonist"]

    # Calculate dynamic numbers based on user's actuals
    dyn_pullups = max(1, math.floor(actuals['pullups'] * 0.8))
    dyn_pushups = max(1, math.floor(actuals['pushups'] * 0.8))
    
    # Calculate 80% and 60% weights (assuming maxhang is used for finger weight, and weighted_pull for pull weight)
    # We add this calculation directly to the string if it detects the phrase
    weight_80_finger = math.floor(actuals['maxhang'] * 0.8)
    weight_100_finger = math.floor(actuals['maxhang'] * 1.0)
    weight_60_pull = math.floor(actuals['weighted_pull'] * 0.6)

    # Output Header
    print("struct{ Name : weight : sets : reps : time for exercise : rest time\n")
    
    schedule = {day: [] for day in active_days}
    
    pool_of_exercises = []
    for cat in prescriptions:
        available = exercise_bank[cat]
        selected = random.sample(available, min(2, len(available)))
        pool_of_exercises.extend(selected)
        
    while len(pool_of_exercises) < (len(active_days) * 4): # Target 4 exercises per session
        extra_cat = random.choice(["finger_strength", "pulling_strength", "antagonist", "finger_endurance"])
        extra_ex = random.choice(exercise_bank[extra_cat])
        if extra_ex not in pool_of_exercises:
            pool_of_exercises.append(extra_ex)

    for i, ex_dict in enumerate(pool_of_exercises):
        day = active_days[i % len(active_days)]
        ex_instance = ex_dict.copy()
        
        # Inject dynamic Reps
        if ex_instance['reps'] == "DYNAMIC_PULLUP_REP":
            ex_instance['reps'] = f"{dyn_pullups}"
        elif ex_instance['reps'] == "DYNAMIC_PUSHUP_REP":
            ex_instance['reps'] = f"{dyn_pushups}"
            
        # Inject dynamic Weights
        if "80% of max weight" in ex_instance['weight'] and ex_instance['name'] in ["Pinch block", "Half crimp lift"]:
            ex_instance['weight'] = f"80% of max weight ({weight_80_finger}lbs)"
        elif "100% of max weight" in ex_instance['weight']:
            ex_instance['weight'] = f"100% of max weight ({weight_100_finger}lbs)"
        elif "60% of max weight" in ex_instance['weight']:
            ex_instance['weight'] = f"60% of max weight ({weight_60_pull}lbs)"
            
        schedule[day].append(ex_instance)
        
    # Print the Schedule in the exact requested schema
    for day in active_days:
        print(f"{day}[")
        # Loop through each exercise for the day
        for j, ex in enumerate(schedule[day]):
            # Check if it's the last item to format the comma correctly
            comma = "," if j < len(schedule[day]) - 1 else ","
            print(f"    {ex['name']} : {ex['weight']} : {ex['sets']} : {ex['reps']} : {ex['time']} : {ex['rest']}{comma}")
        print("]")
        print() # Empty line between days

# --- 4. Function Call (THIS TRIGGERS THE SCRIPT) ---
user_actuals = {
    'pullups': 8.0,            
    'weighted_pull': 120.0,    
    'maxhang': 80.0,           
    'continuoushang': 15.0,    
    'powl': 20.0,              
    'powr': 20.0,              
    'pushups': 20.0            
}

# Run it!
generate_schema_plan(
    target_grade=8.0, 
    sex=1.0, 
    height=70.0, 
    weight=160.0, 
    actuals=user_actuals, 
    active_days=["Monday", "Wednesday", "Friday"]
) 