from flask import Flask, request, jsonify
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
import random
import math
import warnings

warnings.filterwarnings('ignore')

# Initialize the Flask App
app = Flask(__name__)

# --- 1. Exercise Bank (Keep your existing exercise_bank dictionary here) ---
# ... [Insert exercise_bank here] ...

# --- 2. Train AI (Runs once when the server starts) ---
df = pd.read_csv("reducedAssesmentData.csv")
X = df[['bouldergrade_numeric', 'sex_encoded', 'height', 'weight']]
y_cols = ['pullup reps', 'weightedpull', 'maxhang', 'continuoushang', 'repeaters1', 'powl', 'powr', 'pushup reps']
y = df[y_cols].fillna(0)

ai_model = RandomForestRegressor(random_state=42)
ai_model.fit(X, y)

# --- 3. The Logic Layer (Modified to RETURN text instead of printing) ---
def generate_schema_plan(target_grade, sex, height, weight, actuals, active_days):
    # ... [Keep all your existing math and logic here] ...
    
    # INSTEAD OF PRINTING, WE BUILD A STRING:
    output_text = "struct{ Name : weight : sets : reps : time for exercise : rest time\n\n"
    
    # ... [Keep your pool building and scheduling logic here] ...
    
    # Build the string output
    for day in active_days:
        output_text += f"{day}[\n"
        for j, ex in enumerate(schedule[day]):
            comma = "," if j < len(schedule[day]) - 1 else ","
            output_text += f"    {ex['name']} : {ex['weight']} : {ex['sets']} : {ex['reps']} : {ex['time']} : {ex['rest']}{comma}\n"
        output_text += "]\n\n"
        
    return output_text

# --- 4. The API Endpoint ---
@app.route('/generate_plan', methods=['POST'])
def generate_plan_api():
    # 1. Receive data from Android
    data = request.get_json()
    
    # 2. Extract variables
    target_grade = data['target_grade']
    sex = data['sex']
    height = data['height']
    weight = data['weight']
    actuals = data['actuals']
    active_days = data['active_days']
    
    # 3. Run the generator
    plan_string = generate_schema_plan(target_grade, sex, height, weight, actuals, active_days)
    
    # 4. Send back to Android
    return jsonify({"training_plan": plan_string})

# Run the server
if __name__ == '__main__':
    # Runs locally on port 5000
    app.run(host='0.0.0.0', port=5000, debug=True)