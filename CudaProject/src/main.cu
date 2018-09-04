#include <algorithm>
#include <ctime>
#include <cuda_runtime_api.h>
#include <fstream>
#include <iostream>
#include <list>
#include <map>
#include <numeric>
#include <sstream>
#include <string>
#include <stdio.h>
#include <stdlib.h>

using namespace std;

static void CheckCudaErrorAux(const char *, unsigned, const char *,
		cudaError_t);
#define CUDA_CHECK_RETURN(value) CheckCudaErrorAux(__FILE__,__LINE__, #value, value)


bool FIND_BIGRAM = false; //true = find brigrams; false = find trigrams
bool PRINT = false; //if set to true it will print the found bigrams and trigrams
int GRID_DIM = 10; // grid size
int BLOCK_DIM = 128; //block size
std::string nameFile = "inputTextLong.txt"; //the name of the text file to analyse


// this utility method allows the user to better understand the CUDA errors
static void CheckCudaErrorAux(const char *file, unsigned line,
		const char *statement, cudaError_t err) {
	if (err == cudaSuccess)
		return;
	std::cerr << statement << " returned " << cudaGetErrorString(err) << "("
			<< err << ") at " << file << ":" << line << std::endl;
	exit(1);
}

// converts the passed text line into only lower case alphabet characters
__host__ string clean(string in) {
	string final;
	for(int i = 0; i < in.length(); i++) {
		if(isalpha(in[i])) final += tolower(in[i]);
	}
	return final;
}

// this method finds the graphems (bigram or trigram) using the CPU
__host__ void findGraphemsWithCPU(string line, std::map<std::string,int> &graphems) {

	int tail = FIND_BIGRAM? 1 : 2;

	for(int i = 0; i < line.length()-tail; i++) {

		string key = std::string() + line[i] + line[i+1];
		if(!FIND_BIGRAM)
			key = key + line[i+2];

		std::map<std::string,int>::iterator it = graphems.find(key);
		if(it != graphems.end()){
			it->second++;
		}else{
			graphems.insert(std::make_pair(key, 1));
		}

	}

}

// this method finds the graphems (bigram or trigram) using the CPU
__host__ std::map<std::string,int> methodWithCPU(std::string line){
	std::map<std::string,int> graphems;

	findGraphemsWithCPU(line,graphems);

	return graphems;

}

// this method converts a character into an int
__device__ int getCharIndex(char c){
	return (c - 'a');
}

//this method finds the graphems (bigram or trigram) using the GPU
__global__ void findGraphemsWithGPU(const char *line, int* graphemsArray, int sliceLength, int lineLength, bool findBigram) {

	int startPoint =
			blockDim.x * blockIdx.x +
			threadIdx.x;

	startPoint *= sliceLength;

	int endPoint = startPoint + sliceLength - 1;
	int tail = findBigram? 1 : 2;
	endPoint += tail;

	int index1;
	int index2;
	int index3;
	if((startPoint+tail) < lineLength ){
		index2 = getCharIndex(line[startPoint]);
		if(!findBigram) {
			index3 = getCharIndex(line[startPoint+1]);
		}
	}


	while((startPoint+tail) <= endPoint && (startPoint+tail) < lineLength){
		index1 = index2;
		if(findBigram) {
			index2 = getCharIndex(line[startPoint+tail]);
			atomicAdd(&graphemsArray[index1 * 26 + index2 ], 1);
		}else{
			index2 = index3;
			index3 = getCharIndex(line[startPoint+tail]);
			atomicAdd(&graphemsArray[index1 * 26 * 26 + index2 * 26 + index3], 1);
		}

		startPoint++;
	}

	return;
}

// this method prints the graphems found with the GPU
__host__ void print(int *graphemsArrayHost){
	int lengthGraphems = FIND_BIGRAM? 26*26 : 26*26*26;
	std::string alphabet = "abcdefghijklmnopqrstuvwxyz";
	for(int i = 0 ; i < lengthGraphems; i++){
		if(graphemsArrayHost[i] != 0){
			div_t result1 = std::div(i,26);
			div_t result2 = std::div(result1.quot,26);
			if(FIND_BIGRAM){
				cout << (std::string() + alphabet[result2.rem]+ alphabet[result1.rem]) << " = " << graphemsArrayHost[i] << "\n";
			}else{
				div_t result3 = std::div(result2.quot,26);
				cout << (std::string() + alphabet[result3.rem]+ alphabet[result2.rem] + alphabet[result1.rem]) << " = " << graphemsArrayHost[i] << "\n";
			}
		}
	}
}

// this method finds the graphems (bigram or trigram) using the GPU
__host__ int* methodWithGPU(std::string line){

	// GRAPHEMS ARRAY
	int lengthGraphems = FIND_BIGRAM? 26*26 : 26*26*26;
	int *graphemsArrayDevice;
	int *graphemsArrayHost=(int*)calloc(lengthGraphems,sizeof(int));


	//	allocate device memory
	CUDA_CHECK_RETURN(
			cudaMalloc((void ** )&graphemsArrayDevice,
					sizeof(int) * lengthGraphems));

	//	copy from host to device memory
	CUDA_CHECK_RETURN(
			cudaMemcpy(graphemsArrayDevice, graphemsArrayHost, lengthGraphems * sizeof(int),
					cudaMemcpyHostToDevice));

	//  TEXT LINE
	int lengthLine = line.length();
	char *lineDevice;

	//  allocate device memory
	CUDA_CHECK_RETURN(
			cudaMalloc((void ** )&lineDevice,
					sizeof(char) * lengthLine));
	//
	//	copy from host to device memory
	CUDA_CHECK_RETURN(
			cudaMemcpy(lineDevice, line.c_str(), lengthLine * sizeof(char),
					cudaMemcpyHostToDevice));


	// execute kernel
	int totalthreadNumber = GRID_DIM * BLOCK_DIM;
	int sliceLength = ceil(float(lengthLine)/float(totalthreadNumber));
	findGraphemsWithGPU<<< GRID_DIM, BLOCK_DIM >>>(lineDevice, graphemsArrayDevice, sliceLength, lengthLine, FIND_BIGRAM);
	//
	cudaDeviceSynchronize();

	//	copy results from device memory to host
	CUDA_CHECK_RETURN(
			cudaMemcpy(graphemsArrayHost, graphemsArrayDevice, lengthGraphems * sizeof(int),
					cudaMemcpyDeviceToHost));


	// Free the GPU memory here
	cudaFree(lineDevice);
	cudaFree(graphemsArrayDevice);
	return graphemsArrayHost;

}

// The main method.
// Parameters:
// 1 - [b,t] in order to chose between "Bigrams" or "Trigrams" (default: b)
// 2 - size of grid for the initial call (default: 10)
// 3 - size of block for the initial call (default: 128)
// 4 - [t,f,true,false] to print the result of the graphems (default: false)
// 5 - the name of the input file (default: inputTextLong.txt)
//
// calling example: ./main t 5 32 true inputTextVeryLong.txtx
__host__ int main(int argc, char** argv) {
	if(argc > 1){
		std::string setting(argv[1]);
		if(setting == "b" ) {
			FIND_BIGRAM = true;
		}else if(setting == "t" ) {
			FIND_BIGRAM = false;
		}else{
			cout<<"Parameter "<< argv[1] <<" not accepted. Only \"b\" (bigram), \"t\" (trigram), accepted. "<< "\n";
			return 0;
		}
		if(argc > 2){
			GRID_DIM = atoi(argv[2]);
			if(argc > 3){
				BLOCK_DIM = atoi(argv[3]);
				if(argc > 4){
					std::string setting(argv[4]);
					if (setting == "t" || setting == "true")
						PRINT = true;
					if(argc > 5){
						std::string setting(argv[5]);
						nameFile = setting;
					}
				}
			}
		}
	}


	std::string line;
	std::string longLine;
	std::string path = "input/"+nameFile;
	ifstream myfile(path.c_str());
	if (myfile.is_open()) {
		while (getline(myfile, line)) {
			//	Cleaning the line
			line = clean(line);
			longLine += line;

		}
		myfile.close();
	}

	else
		cout << "Unable to open file";


	clock_t beginCPU = clock();
	std::map<std::string,int> graphems;
	graphems = methodWithCPU(longLine);
	clock_t endCPU = clock();

	//	showing contents:
	cout<< "GRID_DIM: " << GRID_DIM << ", BLOCK_DIM: " << BLOCK_DIM << "\n";
	double elapsed_secsCPU = double(endCPU - beginCPU) / CLOCKS_PER_SEC;
	cout<<"CPU METHOD: " << "\n";
	cout<<"Elapsed milliseconds: " << elapsed_secsCPU*1000 << "\n";
	cout<<"Microseconds: " << endCPU - beginCPU << "\n";

	// ITERATION TO START COMUNICATION WITH GPU
	int *graphemsArrayHost;
	clock_t beginGPU = clock();
	graphemsArrayHost = methodWithGPU(longLine);
	clock_t endGPU = clock();
	// Free host memory
	double elapsed_secsGPU =  double(endGPU - beginGPU) / CLOCKS_PER_SEC;
	std::cout << "FIRST ITERATION. GRID_DIM: " << GRID_DIM << ", BLOCK_DIM: " << BLOCK_DIM << "\n";
	std::cout << "Elapsed Milliseconds: " << elapsed_secsGPU*1000 << "\n";

	//verify data
	if(PRINT){
		std::cout << "The graphems obtained with CPU are:\n";
		std::map<std::string,int>::iterator it;
		for (it=graphems.begin(); it!=graphems.end(); ++it)
			std::cout << it->first << " => " << it->second << '\n';

		std::cout << "\n\n -----------------------------------------\n\n";
		std::cout << "The graphems obtained with GPU are:\n";
		print(graphemsArrayHost);
	}
	free(graphemsArrayHost);


	std::cout << "Elapsed milliseconds changing grid dimension and block dimension: \n";

	for (int dimBlocco=1; dimBlocco <= 512 ; dimBlocco = dimBlocco*2 ){
		std::cout << "," << dimBlocco;
	}


	std::cout << "\n\n";
	for (int dimGriglia=1; dimGriglia <= 512 ; dimGriglia = dimGriglia*2 ){
		GRID_DIM = dimGriglia;
		std::cout << dimGriglia;
		for (int dimBlocco=1; dimBlocco <= 512 ; dimBlocco = dimBlocco*2 ){
			BLOCK_DIM = dimBlocco;
			int *graphemsArrayHost;
			clock_t beginGPU = clock();
			graphemsArrayHost = methodWithGPU(longLine);
			clock_t endGPU = clock();

			// Free host memory
			free(graphemsArrayHost);

			double elapsed_secsGPU =  double(endGPU - beginGPU) / CLOCKS_PER_SEC;
			std::cout << ", "<< elapsed_secsGPU*1000 ;
		}
		std::cout <<  "\n";
	}
	return 0;
}
