import pandas as pd
import re

def normalize_boulder_grade(grade_str):
    if not isinstance(grade_str, str) or grade_str.lower() == 'nan':
        return None
    
    # Clean string: handle "<V3" by just taking "V3", convert to upper
    clean_str = re.sub(r'[<>]', '', grade_str).strip().upper()
    
    match = re.search(r'V(\d+)(\+?)', clean_str)
    if match:
        val = int(match.group(1))
        if match.group(2) == '+':
            val += 0.5
        return float(val)
    return None

def normalize_sport_grade(grade_str):
    if not isinstance(grade_str, str) or grade_str.lower() == 'nan':
        return None
    
    # Clean string
    s = grade_str.strip().lower()
    
    # Regex to capture the number and the letter(s)
    match = re.search(r'5\.(\d+)([a-d]?[/]?[a-d]?)', s)
    if not match:
        return None
        
    base = int(match.group(1))
    suffix = match.group(2)
    
    # Mapping letters to values
    letter_map = {'a': 0.25, 'b': 0.50, 'c': 0.75, 'd': 1.00}
    
    if '/' in suffix:
        l1, l2 = suffix.split('/')
        val = (letter_map.get(l1, 0) + letter_map.get(l2, 0)) / 2
    else:
        val = letter_map.get(suffix, 0)
        
    return float(base + val)

# Apply to your dataset
df = pd.read_csv('workout-generator\__Final_Data.csv')

df['bouldergrade_numeric'] = df['bouldergrade'].apply(normalize_boulder_grade)
df['sportgrade_numeric'] = df['sportgrade'].apply(normalize_sport_grade)
df['max_boulder_numeric'] = df['max_boulder'].apply(normalize_boulder_grade)
df['max_sport_numeric'] = df['max_sport'].apply(normalize_sport_grade)

# Print a preview to the console so you can verify it worked
print("Previewing normalized data:")
print(df[['max_boulder', 'max_boulder_numeric', 'max_sport', 'max_sport_numeric']].head())

# Save the updated dataset to a new CSV file
df.to_csv('normalized_data.csv', index=False)
print("\nSuccess: Saved to 'normalized_data.csv'")