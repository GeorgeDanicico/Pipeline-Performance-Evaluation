import random
import string
import csv
from datetime import datetime

def generate_random_string(length=20):
    """Generate a random string of fixed length."""
    letters = string.ascii_letters + string.digits
    return ''.join(random.choice(letters) for _ in range(length))

def generate_ycsb_record():
    """Generate a single YCSB record with random fields."""
    record = {
        'userid': f'user{random.randint(1, 1000000):06d}',
        'field0': generate_random_string(),
        'field1': generate_random_string(),
        'field2': generate_random_string(),
        'field3': generate_random_string(),
        'field4': generate_random_string(),
        'field5': generate_random_string(),
        'field6': generate_random_string(),
        'field7': generate_random_string(),
        'field8': generate_random_string(),
        'field9': generate_random_string()
    }
    return record

def generate_ycsb_dataset(num_records, output_file):
    """Generate YCSB dataset and write to CSV file."""
    fieldnames = ['userid', 'field0', 'field1', 'field2', 'field3', 
                 'field4', 'field5', 'field6', 'field7', 'field8', 'field9']
    
    with open(output_file, 'w', newline='') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        
        for _ in range(num_records):
            record = generate_ycsb_record()
            writer.writerow(record)

if __name__ == "__main__":
    # Number of records to generate
    NUM_RECORDS = 10000  # 1 million records
    
    # Generate timestamp for unique filename
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_file = f'ycsb_data_{timestamp}.csv'
    
    print(f"Generating {NUM_RECORDS} YCSB records...")
    generate_ycsb_dataset(NUM_RECORDS, output_file)
    print(f"Data has been written to {output_file}") 