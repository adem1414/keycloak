const ERROR_FIELDS = ["error", "errorMessage"];

export type NetworkErrorOptions = { response: Response; responseData: unknown };

export class NetworkError extends Error {
  response: Response;
  responseData: unknown;

  constructor(message: string, options: NetworkErrorOptions) {
    super(message);
    this.response = options.response;
    this.responseData = options.responseData;
  }
}

export async function fetchWithError(
  input: Request | string | URL,
  init?: RequestInit,
) {
  const response = await fetch(input, init);

  if (!response.ok) {
    const responseData = await parseResponse(response);
    const baseMessage = getErrorMessage(responseData);

    // make a concise representation of responseData for the message
    let dataPreview: string;
    try {
      dataPreview = typeof responseData === "string"
        ? responseData
        : JSON.stringify(responseData);
    } catch {
      dataPreview = "[unserializable responseData]";
    }

    if (dataPreview.length > 500) {
      dataPreview = dataPreview.slice(0, 500) + "...";
    }

    const message = `${baseMessage} (status: ${response.status}) ${dataPreview}`;

    throw new NetworkError(message, {
      response,
      responseData,
    });
  }

  return response;
}

export async function parseResponse(response: Response): Promise<any> {
  if (!response.body) {
    return "";
  }

  const data = await response.text();

  try {
    return JSON.parse(data);
  } catch {
    return data;
  }
}

function getErrorMessage(data: unknown): string {
  if (typeof data !== "object" || data === null) {
    return "Unable to determine error message.";
  }

  for (const key of ERROR_FIELDS) {
    const value = (data as Record<string, unknown>)[key];

    if (typeof value === "string") {
      return value;
    }
  }

  return "Network response was not OK.";
}
