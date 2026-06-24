/*
 *  Copyright (c), 2026 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package basilica2.agents.events;

import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Fired by WebsocketChatClient when it receives a multimodal updatechat
 * message containing a cameraframe::: tag.  Carries the raw base64 JPEG
 * and all associated metadata so downstream listeners (e.g. a vision/OCR
 * component) can process the image without re-parsing the wire format.
 */
public class ImageEvent extends Event
{

	public static String GENERIC_NAME = "IMAGE_EVENT";
	
	@Override
	public String getName()
	{
		return GENERIC_NAME;
	}

    /** Base64-encoded image data (no "data:..." prefix). */
    private final String imageBase64;

    /** MIME type, e.g. "image/jpeg". */
    private final String mimeType;

    /** Capture width in pixels (0 if unknown). */
    private final int width;

    /** Capture height in pixels (0 if unknown). */
    private final int height;

    /** Optional problem/activity identifier supplied by the phone UI. */
    private final String problemId;

    /** Monotonically increasing frame counter within this session. */
    private final int frameCount;

    /** Socket.IO username of the sender — always "CameraPhone" for now. */
    private final String senderUsername;

    public ImageEvent(Component source,
                      String senderUsername,
                      String imageBase64,
                      String mimeType,
                      int width,
                      int height,
                      String problemId,
                      int frameCount)
    {
        super(source);
        this.senderUsername  = senderUsername  != null ? senderUsername  : "CameraPhone";
        this.imageBase64     = imageBase64     != null ? imageBase64     : "";
        this.mimeType        = mimeType        != null ? mimeType        : "image/jpeg";
        this.width           = width;
        this.height          = height;
        this.problemId       = problemId       != null ? problemId       : "";
        this.frameCount      = frameCount;
    }

    public String getImageBase64()    { return imageBase64;    }
    public String getMimeType()       { return mimeType;       }
    public int    getWidth()          { return width;          }
    public int    getHeight()         { return height;         }
    public String getProblemId()      { return problemId;      }
    public int    getFrameCount()     { return frameCount;     }
    public String getSenderUsername() { return senderUsername; }

    @Override
    public String toString()
    {
        return "ImageEvent[frame=" + frameCount
             + " size=" + width + "x" + height
             + " mime=" + mimeType
             + " problem=" + problemId
             + " from=" + senderUsername
             + " base64len=" + imageBase64.length() + "]";
    }
}
