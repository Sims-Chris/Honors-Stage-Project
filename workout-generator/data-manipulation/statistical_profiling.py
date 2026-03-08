import pandas as pd
import numpy as np
import re

# 1. Load the dataset
# Make sure to use the correct file path for your machine
df = pd.read_csv(r'workout-generator\\Data\\Filled_AssessmentDataSept2022.csv')

# Clean up column names just in case there are hidden spaces
df.columns = df.columns.str.strip()

# 2. Create mapping functions to convert text grades to numeric values
def map_boulder_grade(grade):
    """ Converts text boulder grades into a numeric scale. """
    grade = str(grade).strip().upper()
    
    if 'DO NOT' in grade or grade == 'NAN':
        return np.nan
    elif '<V3' in grade:
        return 2.0  
    else:
        match = re.search(r'\d+', grade)
        if match:
            return float(match.group())
        return np.nan

def map_sport_grade(grade):
    """ Converts text sport grades into an increasing numeric scale. """
    grade = str(grade).strip().lower()
    
    if '< 5.10' in grade or '<5.10' in grade:
        return 9.0
    elif '5.10a/b' in grade:
        return 10.0
    elif '5.10c/d' in grade:
        return 10.5
    elif '5.11a/b' in grade:
        return 11.0
    elif '5.11c/d' in grade:
        return 11.5
    elif '5.12a/b' in grade:
        return 12.0
    elif '5.12c/d' in grade:
        return 12.5
    elif '5.13a/b' in grade:
        return 13.0
    elif '5.13c/d' in grade:
        return 13.5
    elif '5.14a/b' in grade:
        return 14.0
    elif '5.14c/d' in grade:
        return 14.5
    elif '> 5.14d' in grade or '>5.14d' in grade:
        return 15.0
    else:
        return np.nan 

# 3. Create the numeric columns needed for your groupby
df['bouldergrade_numeric'] = df['bouldergrade'].apply(map_boulder_grade)
df['sportgrade_numeric'] = df['sportgrade'].apply(map_sport_grade)

# 4. Define ALL numerical metrics for profiling
metrics = [
    'pullup reps', 'pushup reps', 
    'continuoushang', 'maxhang', 'weightedpull', 'repeaters1', 
    'bigmovecamp', 'onerungcamp', 
    'overheadprR', 'overheadprL', 
    'deadlift', 
    'powl', 'powr', 
    'lhang/sit', 
    'armjump', 'hipjump'
]

# 5. Aggregate for Boulder Grades
boulder_stats = df.groupby('bouldergrade_numeric')[metrics].agg(['mean', 'median', 'std']).reset_index()

# 6. Aggregate for Sport Grades
sport_stats = df.groupby('sportgrade_numeric')[metrics].agg(['mean', 'median', 'std']).reset_index()

# 7. Function to flatten the MultiIndex columns
def flatten_columns(df):
    df.columns = [f"{col[0]}_{col[1]}" if col[1] else col[0] for col in df.columns]
    return df

# Apply to your dataframes
boulder_stats = flatten_columns(boulder_stats)
sport_stats = flatten_columns(sport_stats)

# Now save them
boulder_stats.to_csv('boulder_grade_stats_flat.csv', index=False)
sport_stats.to_csv('sport_grade_stats_flat.csv', index=False)

print("Success! Your comprehensive CSVs with all metrics have been created.")