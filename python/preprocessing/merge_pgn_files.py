import os

def merge_pgn_files(input_directory, output_file):
    with open(output_file, 'w') as outfile:
        for filename in os.listdir(input_directory):
            if filename.endswith('.pgn'):
                with open(os.path.join(input_directory, filename), 'r') as infile:
                    outfile.write(infile.read())
                    outfile.write('\n')

if __name__ == "__main__":
    input_directory = '../data/pgn'
    output_file = '../data/pgn/magnus_carlsen.pgn'
    merge_pgn_files(input_directory, output_file)