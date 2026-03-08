import pandas as pd
from sklearn.ensemble import RandomForestRegressor
import m2cgen as m2c
import warnings

warnings.filterwarnings('ignore')

print("1. Loading dataset...")
df = pd.read_csv("reducedAssesmentData.csv")

# Input features (What the user enters)
X = df[['bouldergrade_numeric', 'sex_encoded', 'height', 'weight']]

# Target metrics (What the AI predicts)
y_cols = ['pullup reps', 'weightedpull', 'maxhang', 'continuoushang', 'repeaters1', 'powl', 'powr', 'pushup reps']
y = df[y_cols].fillna(0)

print("2. Training AI Model for Mobile...")
# Limit trees to keep the mobile file size small and fast
ai_model = RandomForestRegressor(n_estimators=15, max_depth=6, random_state=42)
ai_model.fit(X, y)

print("3. Converting AI to native Java code (This may take a minute)...")
# Translate the model math into Java
java_code = m2c.export_to_java(ai_model, class_name="BoulderingAI")

print("4. Saving to BoulderingAI.java...")
with open("BoulderingAI.java", "w") as f:
    f.write(java_code)

print("Success! You can now drag BoulderingAI.java into Android Studio.")