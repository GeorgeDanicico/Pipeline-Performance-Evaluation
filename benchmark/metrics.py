import matplotlib.pyplot as plt
import numpy as np

def plot_benchmark_results(mongo_metrics, couchbase_metrics):
    """
    Plot benchmark results for MongoDB and Couchbase across different record counts and thread counts
    
    Args:
        mongo_metrics: Dictionary containing MongoDB metrics for different record counts and thread counts
        couchbase_metrics: Dictionary containing Couchbase metrics for different record counts and thread counts
        
    Example metrics structure:
    {
        '10000': {
            '2': {'throughput': 1800, 'exec_time': 8, 'latency': 0.0012},
            '4': {'throughput': 2500, 'exec_time': 6, 'latency': 0.0015}
        },
        '100000': {
            '2': {'throughput': 2000, 'exec_time': 9, 'latency': 0.0013},
            '4': {'throughput': 2800, 'exec_time': 7, 'latency': 0.0016}
        },
        # ... similar for 1000000 and 10000000
    }
    """
    record_counts = ['10000', '100000', '1000000', '10000000']
    thread_counts = ['2', '4']
    x = np.arange(len(record_counts))
    width = 0.35
    
    # Create figure with 3 subplots
    fig, (ax1, ax2, ax3) = plt.subplots(1, 3, figsize=(20, 6))
    
    # Plot for each thread count
    for thread in thread_counts:
        # Prepare data for this thread count
        mongo_throughput = [mongo_metrics[rc][thread]['throughput'] for rc in record_counts]
        couchbase_throughput = [couchbase_metrics[rc][thread]['throughput'] for rc in record_counts]
        
        mongo_exec_time = [mongo_metrics[rc][thread]['exec_time'] for rc in record_counts]
        couchbase_exec_time = [couchbase_metrics[rc][thread]['exec_time'] for rc in record_counts]
        
        mongo_latency = [mongo_metrics[rc][thread]['latency'] for rc in record_counts]
        couchbase_latency = [couchbase_metrics[rc][thread]['latency'] for rc in record_counts]
        
        # Plot 1: Throughput
        ax1.bar(x - width/2 + (0.5 if thread == '4' else 0), mongo_throughput, 
                width/2, label=f'MongoDB {thread} threads', 
                color='blue', alpha=0.7 if thread == '2' else 0.4)
        ax1.bar(x + width/2 + (0.5 if thread == '4' else 0), couchbase_throughput, 
                width/2, label=f'Couchbase {thread} threads', 
                color='red', alpha=0.7 if thread == '2' else 0.4)
        
        # Plot 2: Execution Time
        ax2.bar(x - width/2 + (0.5 if thread == '4' else 0), mongo_exec_time, 
                width/2, label=f'MongoDB {thread} threads', 
                color='blue', alpha=0.7 if thread == '2' else 0.4)
        ax2.bar(x + width/2 + (0.5 if thread == '4' else 0), couchbase_exec_time, 
                width/2, label=f'Couchbase {thread} threads', 
                color='red', alpha=0.7 if thread == '2' else 0.4)
        
        # Plot 3: Latency
        ax3.bar(x - width/2 + (0.5 if thread == '4' else 0), mongo_latency, 
                width/2, label=f'MongoDB {thread} threads', 
                color='blue', alpha=0.7 if thread == '2' else 0.4)
        ax3.bar(x + width/2 + (0.5 if thread == '4' else 0), couchbase_latency, 
                width/2, label=f'Couchbase {thread} threads', 
                color='red', alpha=0.7 if thread == '2' else 0.4)
    
    # Configure plots
    for ax, title, ylabel in [
        (ax1, 'Throughput Comparison', 'Operations per Second'),
        (ax2, 'Execution Time Comparison', 'Time (seconds)'),
        (ax3, 'Latency Comparison', 'Latency (seconds)')
    ]:
        ax.set_title(title)
        ax.set_xlabel('Record Count')
        ax.set_ylabel(ylabel)
        ax.set_xticks(x + width/2)
        ax.set_xticklabels(record_counts)
        ax.grid(True, axis='y')
        ax.legend()
    
    # Add value labels on top of bars
    def add_value_labels(ax, values, offset):
        for i, v in enumerate(values):
            ax.text(i + offset, v, f'{v:.2f}', ha='center', va='bottom')
    
    # Adjust layout and display
    plt.tight_layout()
    plt.show()

# Example usage:
if __name__ == "__main__":
    # Example data structure - replace with your actual values
    mongo_metrics = {
        '10000': {
            '2': {'throughput': 1800, 'exec_time': 8, 'latency': 0.0012},
            '4': {'throughput': 2500, 'exec_time': 6, 'latency': 0.0015}
        },
        '100000': {
            '2': {'throughput': 2000, 'exec_time': 9, 'latency': 0.0013},
            '4': {'throughput': 2800, 'exec_time': 7, 'latency': 0.0016}
        },
        '1000000': {
            '2': {'throughput': 2200, 'exec_time': 10, 'latency': 0.0014},
            '4': {'throughput': 3000, 'exec_time': 8, 'latency': 0.0017}
        },
        '10000000': {
            '2': {'throughput': 2400, 'exec_time': 11, 'latency': 0.0015},
            '4': {'throughput': 3200, 'exec_time': 9, 'latency': 0.0018}
        }
    }
    
    couchbase_metrics = {
        '10000': {
            '2': {'throughput': 2000, 'exec_time': 7, 'latency': 0.0010},
            '4': {'throughput': 2700, 'exec_time': 5, 'latency': 0.0013}
        },
        '100000': {
            '2': {'throughput': 2200, 'exec_time': 8, 'latency': 0.0011},
            '4': {'throughput': 3000, 'exec_time': 6, 'latency': 0.0014}
        },
        '1000000': {
            '2': {'throughput': 2400, 'exec_time': 9, 'latency': 0.0012},
            '4': {'throughput': 3200, 'exec_time': 7, 'latency': 0.0015}
        },
        '10000000': {
            '2': {'throughput': 2600, 'exec_time': 10, 'latency': 0.0013},
            '4': {'throughput': 3400, 'exec_time': 8, 'latency': 0.0016}
        }
    }
    
    plot_benchmark_results(mongo_metrics, couchbase_metrics) 