#import <Foundation/Foundation.h>

@interface StringUtils : NSObject
+ (char*) CStringCopy:(NSString*)input;
+ (NSString*) CreateNSString:(const char*) string;
@end
