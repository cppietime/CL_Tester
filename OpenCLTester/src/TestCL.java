

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.Util;

public class TestCL {
	
	private static final int SIZE = 10000000; //Total number of test elements
	
	private static final int SEL = new Random().nextInt(SIZE); //Random element to check
	
	private static CLContext context; //CL Context
	private static CLPlatform platform; //CL platform
	private static List<CLDevice> devices; //List of CL devices
	private static CLCommandQueue queue; //Command Queue for context
	private static float[] aData, bData, rData; //float arrays to store test data
	
	//---Kernel Code---
	//The actual kernel script is here:
	//-----------------
	private static String kernel = "kernel void sum(global const float* a, global const float* b, global float* result, int const size){\n" + 
			"const int itemId = get_global_id(0);\n" + 
			"if(itemId < size){\n" + 
			"result[itemId] = sin(a[itemId]) * cos(b[itemId]);\n" +
			"}\n" +
			"}";;
	
	public static void main(String[] args){
		
		aData = new float[SIZE];
		bData = new float[SIZE];
		rData = new float[SIZE]; //Only used for CPU testing
		
		//arbitrary testing data
		for(int i=0; i<SIZE; i++){
			aData[i] = i;
			bData[i] = SIZE - i;
		}
		
		try {
			//testCPU(); //How long does it take running in traditional Java code on the CPU?
			testGPU(); //How long does the GPU take to run it w/ CL?
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Test the CPU with pure Java code
	 */
	private static void testCPU(){
		long time = System.currentTimeMillis();
		for(int i=0; i<SIZE; i++){
			rData[i] = (float) (Math.sin(aData[i]) * Math.cos(bData[i]));
		}
		//Print the time FROM THE START OF THE testCPU() FUNCTION UNTIL NOW
		System.out.println("CPU processing time for " + SIZE + " elements: " + (System.currentTimeMillis() - time));
		//Print a random element from rData to test equality with the GPU's result
		System.out.println("CPU result for element number " + SEL + ": " + rData[SEL]);
	}
	
	/**
	 * Test the GPU with OpenCL
	 * @throws LWJGLException
	 */
	private static void testGPU() throws LWJGLException {
		CLInit(); //Initialize CL and CL Objects
		
		//Create the CL Program
		CLProgram program = CL10.clCreateProgramWithSource(context, kernel, null);
		
		int error = CL10.clBuildProgram(program, devices.get(0), "", null);
		Util.checkCLError(error);
		
		//Create the Kernel
		CLKernel sum = CL10.clCreateKernel(program, "sum", null);
		
		//Error checker
		IntBuffer eBuf = BufferUtils.createIntBuffer(1);
		
		//Floatbuffer for the first array of floats
		FloatBuffer aBuf = BufferUtils.createFloatBuffer(SIZE);
		aBuf.put(aData);
		aBuf.rewind();
		CLMem aMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_COPY_HOST_PTR, aBuf, eBuf);
		Util.checkCLError(eBuf.get(0));
		
		//And the second
		FloatBuffer bBuf = BufferUtils.createFloatBuffer(SIZE);
		bBuf.put(bData);
		bBuf.rewind();
		CLMem bMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_COPY_HOST_PTR, bBuf, eBuf);
		Util.checkCLError(eBuf.get(0));
		
		//Memory object to store the result
		CLMem rMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY, SIZE * 4, eBuf);
		Util.checkCLError(eBuf.get(0));
		
		//Get time before setting kernel arguments
		long time = System.currentTimeMillis();
		
		sum.setArg(0, aMem);
		sum.setArg(1, bMem);
		sum.setArg(2, rMem);
		sum.setArg(3, SIZE);
		
		final int dim = 1;
		PointerBuffer workSize = BufferUtils.createPointerBuffer(dim);
		workSize.put(0, SIZE);
//		workSize.put(1, 100);
//		workSize.put(2, 1000);
		
		//Actually running the program
		CL10.clEnqueueNDRangeKernel(queue, sum, dim, null, workSize, null, null, null);
		CL10.clFinish(queue);
		
		//Write results to a FloatBuffer
		FloatBuffer res = BufferUtils.createFloatBuffer(SIZE);
		CL10.clEnqueueReadBuffer(queue, rMem, CL10.CL_TRUE, 0, res, null, null);
		
		//How long did it take?
		//Print the time FROM THE SETTING OF KERNEL ARGUMENTS UNTIL NOW
		System.out.println("GPU processing time for " + SIZE + " elements: " + (System.currentTimeMillis() - time));
		//Print a random element from res to check equality with the CPU's result
		System.out.println("GPU result for element number " + SEL + ": " + res.get(SEL));
		
		//Cleanup objects
		CL10.clReleaseKernel(sum);
		CL10.clReleaseProgram(program);
		CL10.clReleaseMemObject(aMem);
		CL10.clReleaseMemObject(bMem);
		CL10.clReleaseMemObject(rMem);
		
		CLCleanup();
	}
	
	/**
	 * Initialize CL objects
	 * @throws LWJGLException
	 */
	private static void CLInit() throws LWJGLException {
		IntBuffer eBuf = BufferUtils.createIntBuffer(1);
		
		CL.create();
		
		platform = CLPlatform.getPlatforms().get(0);
		devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
		context = CLContext.create(platform, devices, eBuf);
		queue = CL10.clCreateCommandQueue(context, devices.get(0), CL10.CL_QUEUE_PROFILING_ENABLE, eBuf);
		
		Util.checkCLError(eBuf.get(0));
	}
	
	/**
	 * Cleanup after CL completion
	 */
	private static void CLCleanup(){
		CL10.clReleaseCommandQueue(queue);
		CL10.clReleaseContext(context);
		CL.destroy();
	}
	
}
