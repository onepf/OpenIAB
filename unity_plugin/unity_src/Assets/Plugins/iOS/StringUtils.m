#import "StringUtils.h"

@implementation StringUtils

+ (char*) CStringCopy:(NSString*)input {
    const char *string = [input cStringUsingEncoding:NSUTF8StringEncoding];
    char *result = NULL;
    if (string != NULL) {
        result = (char*)malloc(strlen(string) + 1);
        strcpy(result, string);
    }
    return result;
}

+ (NSString*) CreateNSString:(const char*) string {
    NSString *result = @"";
    if (string != NULL) {
        result = [NSString stringWithUTF8String:string];
    }
    return result;
}

@end
