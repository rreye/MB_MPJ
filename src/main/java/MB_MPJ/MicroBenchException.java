/*
 * Copyright (c) 2026 Universidade da Coruña
 * SPDX-License-Identifier: MIT
 */

package MB_MPJ;

public class MicroBenchException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	MicroBenchException() 
	{
		super();
	}

	MicroBenchException(String msg) 
	{
		super(msg);
	}

	MicroBenchException(Throwable cause)
	{ 
		super(cause);	  
	}

	MicroBenchException(String s, Throwable cause) 
	{
		super(s, cause);
	}
}
